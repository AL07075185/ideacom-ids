package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;
import mx.ideacom.ids.api.dto.EventoDTO;
import mx.ideacom.ids.api.dto.ResumenDTO;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.IncidenciaRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.common.modelo.EstadoRegistro;

@ApplicationScoped
@Transactional
public class DashboardServicio {

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private IncidenciaRepositorio incidencias;

    @Inject
    private DispositivoServicio dispositivoServicio;

    public ResumenDTO resumen(Sujeto s) {
        Integer idResponsable = s.admin() ? null : s.id();
        Map<String, Long> porEstado = dispositivos.contarPorEstado(idResponsable);
        long total = porEstado.values().stream().mapToLong(Long::longValue).sum();

        // Promedio de disponibilidad 24 h de los equipos con agente
        List<Dispositivo> aprobados = dispositivos.listar(EstadoRegistro.APROBADO, idResponsable);
        OptionalDouble promedio = aprobados.stream()
                .filter(Dispositivo::isConAgente)
                .map(d -> dispositivoServicio.calcularDisponibilidad(d.getId(), 24))
                .filter(OptionalDouble::isPresent)
                .mapToDouble(OptionalDouble::getAsDouble)
                .average();
        Double disponibilidad = promedio.isPresent() ? Math.round(promedio.getAsDouble() * 100.0) / 100.0 : null;

        List<EventoDTO> ultimos = eventos.ultimos(10, idResponsable).stream().map(EventoDTO::de).toList();
        return new ResumenDTO(total, porEstado,
                s.admin() ? dispositivos.contarPendientes() : 0,
                incidencias.contarNoCerradas(idResponsable),
                disponibilidad, ultimos);
    }
}
