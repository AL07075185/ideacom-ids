package mx.ideacom.ids.api.reporte;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.EstadoIncidencia;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.Incidencia;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.IncidenciaRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.api.servicio.DispositivoServicio;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.reglas.DisponibilidadCalculadora;
import mx.ideacom.ids.common.util.Fechas;

/** Arma el modelo del reporte de disponibilidad (capa de servicio del MVC). */
@ApplicationScoped
@Transactional
public class ReporteServicio {

    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private IncidenciaRepositorio incidencias;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private DispositivoServicio dispositivoServicio;

    public Reporte generar(Sujeto s, Integer idSitio, int horas, ZoneId zona) {
        Integer idResponsable = s.admin() ? null : s.id();
        LocalDateTime desde = Fechas.ahoraUtc().minusHours(horas);

        List<Dispositivo> lista = dispositivos.listar(EstadoRegistro.APROBADO, idResponsable).stream()
                .filter(d -> idSitio == null || d.getSitio().getId().equals(idSitio))
                .toList();
        Set<Integer> ids = new HashSet<>();
        List<Reporte.Fila> filas = new ArrayList<>();
        long desconexiones = 0;
        long minutosCaido = 0;
        List<Double> porcentajes = new ArrayList<>();

        for (Dispositivo d : lista) {
            ids.add(d.getId());
            Double porcentaje = null;
            long caidas = 0;
            String tiempo = "—";
            if (d.isConAgente()) {
                DisponibilidadCalculadora.Medicion m = dispositivoServicio.medir(d.getId(), horas);
                OptionalDouble p = m.porcentaje();
                porcentaje = p.isPresent() ? p.getAsDouble() : null;
                if (porcentaje != null) {
                    porcentajes.add(porcentaje);
                }
                caidas = eventos.contarDesde(d.getId(), TipoEvento.DESCONEXION_INESPERADA, desde);
                tiempo = duracion(m.minutosCaido());
                desconexiones += caidas;
                minutosCaido += m.minutosCaido();
            }
            filas.add(new Reporte.Fila(d.getNombre(), d.getTipo().name(), d.getIp(), d.getSitio().getNombre(),
                    d.getDepartamento() == null ? "—" : d.getDepartamento().getNombre(),
                    d.getEstadoActual().name(), porcentaje, caidas, tiempo,
                    d.isConAgente() ? fecha(d.getUltimoLatido(), zona) : "Sin agente"));
        }

        List<Reporte.IncidenciaAbierta> abiertas = new ArrayList<>();
        for (Incidencia i : incidencias.listar(null, idResponsable)) {
            boolean delAlcance = i.getDispositivo() == null ? idSitio == null : ids.contains(i.getDispositivo().getId());
            if (i.getEstado() != EstadoIncidencia.CERRADA && delAlcance) {
                abiertas.add(new Reporte.IncidenciaAbierta(i.getId(), i.getTitulo(), i.getPrioridad().name(),
                        i.getEstado().name(), i.getDispositivo() == null ? "General" : i.getDispositivo().getNombre(),
                        fecha(i.getFechaCreacion(), zona)));
            }
        }

        List<Reporte.EventoRelevante> relevantes = new ArrayList<>();
        for (Evento e : eventos.relevantesDesde(desde, 300)) {
            if (ids.contains(e.getDispositivo().getId()) && relevantes.size() < 50) {
                relevantes.add(new Reporte.EventoRelevante(fecha(e.getFechaHora(), zona),
                        e.getDispositivo().getNombre(), nombreEvento(e.getTipo()),
                        e.getEstadoResultante() == null ? null : e.getEstadoResultante().name(), e.getDetalle()));
            }
        }

        Reporte r = new Reporte();
        r.setGenerado(fecha(Fechas.ahoraUtc(), zona));
        r.setZona(zona.getId());
        r.setUsuario(s.nombre());
        r.setHoras(horas);
        r.setPeriodo(horas == 24 ? "Últimas 24 horas" : "Últimos " + (horas / 24) + " días");
        r.setIdSitio(idSitio);
        r.setSitios(sitios.listar().stream().map(x -> new Reporte.OpcionSitio(x.getId(), x.getNombre())).toList());
        r.setSitio(idSitio == null ? "Todos los sitios"
                : sitios.buscarPorId(idSitio).map(x -> x.getNombre()).orElse("Sitio desconocido"));
        r.setFilas(filas);
        r.setIncidencias(abiertas);
        r.setEventos(relevantes);
        r.setPromedio(porcentajes.isEmpty() ? null
                : Math.round(porcentajes.stream().mapToDouble(Double::doubleValue).average().orElse(0) * 100.0) / 100.0);
        r.setTotalDesconexiones(desconexiones);
        r.setTiempoCaidoTotal(duracion(minutosCaido));
        return r;
    }

    static String duracion(long minutos) {
        if (minutos <= 0) {
            return "0 min";
        }
        long h = minutos / 60;
        long m = minutos % 60;
        return h == 0 ? m + " min" : (m == 0 ? h + " h" : h + " h " + m + " min");
    }

    private static String fecha(LocalDateTime utc, ZoneId zona) {
        return utc == null ? "—" : utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(zona).format(FORMATO);
    }

    private static String nombreEvento(TipoEvento t) {
        return switch (t) {
            case DESCONEXION_INESPERADA -> "Desconexión inesperada";
            case APAGADO -> "Apagado";
            case SIN_INTERNET -> "Perdió internet";
            case SIN_CONEXION_SERVIDOR -> "Periodo sin comunicación";
            default -> t.name();
        };
    }
}
