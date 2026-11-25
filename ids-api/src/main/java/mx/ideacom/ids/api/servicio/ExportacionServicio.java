package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.util.Arrays;
import java.util.List;
import mx.ideacom.ids.api.dto.EventoDTO;
import mx.ideacom.ids.api.dto.FilaDispositivoDTO;
import mx.ideacom.ids.api.dto.IncidenciaDTO;
import mx.ideacom.ids.api.entidad.EstadoIncidencia;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.IncidenciaRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.util.Fechas;

/** Exportacion de dispositivos, incidencias e historial a CSV o JSON. */
@ApplicationScoped
@Transactional(Transactional.TxType.SUPPORTS)
public class ExportacionServicio {

    private static final List<String> COLUMNAS_INCIDENCIAS = List.of("id", "titulo", "descripcion", "prioridad",
            "estado", "dispositivo", "creadaPor", "asignadaA", "fechaCreacion", "fechaActualizacion", "fechaCierre");
    private static final List<String> COLUMNAS_EVENTOS = List.of("fechaHora", "dispositivo", "tipo",
            "estadoResultante", "origen", "fechaFin", "detalle");

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private IncidenciaRepositorio incidencias;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private DispositivoServicio dispositivoServicio;

    @Transactional
    public List<FilaDispositivoDTO> dispositivos(Sujeto s, String registro) {
        EstadoRegistro filtro = Textos.enumeracion(EstadoRegistro.class, registro, "registro");
        return dispositivos.listar(filtro, s.admin() ? null : s.id()).stream()
                .map(d -> FilaDispositivoDTO.de(d, false)).toList();
    }

    public String dispositivosCsv(List<FilaDispositivoDTO> filas) {
        return Csv.escribir(FilaDispositivoDTO.COLUMNAS, filas.stream().map(FilaDispositivoDTO::valores).toList());
    }

    @Transactional
    public List<IncidenciaDTO> incidencias(Sujeto s, String estado) {
        EstadoIncidencia filtro = Textos.enumeracion(EstadoIncidencia.class, estado, "estado");
        return incidencias.listar(filtro, s.admin() ? null : s.id()).stream().map(IncidenciaDTO::de).toList();
    }

    public String incidenciasCsv(List<IncidenciaDTO> lista) {
        return Csv.escribir(COLUMNAS_INCIDENCIAS, lista.stream().map(i -> Arrays.<Object>asList(i.id(), i.titulo(),
                i.descripcion(), i.prioridad(), i.estado(), i.dispositivo(), i.creadaPor(), i.asignadaA(),
                i.fechaCreacion(), i.fechaActualizacion(), i.fechaCierre())).toList());
    }

    /** Historial de un dispositivo de los ultimos {@code dias} (respeta los permisos del usuario). */
    @Transactional
    public List<EventoDTO> eventos(Sujeto s, Integer idDispositivo, int dias) {
        dispositivoServicio.obtener(s, idDispositivo);
        int d = Math.max(1, Math.min(dias, 366));
        return eventos.deDispositivoDesde(idDispositivo, Fechas.ahoraUtc().minusDays(d)).stream()
                .map(EventoDTO::de).toList();
    }

    public String eventosCsv(List<EventoDTO> lista) {
        return Csv.escribir(COLUMNAS_EVENTOS, lista.stream().map(e -> Arrays.<Object>asList(e.fechaHora(),
                e.dispositivo(), e.tipo(), e.estadoResultante(), e.origen(), e.fechaFin(), e.detalle())).toList());
    }
}
