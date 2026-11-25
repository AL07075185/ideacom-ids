package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.OrigenEvento;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.reglas.ReglasEstado;
import mx.ideacom.ids.common.util.Fechas;

/** Revisa que los equipos aprobados sigan enviando latidos. */
@ApplicationScoped
@Transactional
public class MonitorServicio {

    private static final Logger LOG = Logger.getLogger(MonitorServicio.class.getName());

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private ConfiguracionServicio configuracion;

    /**
     * Marca como DESCONECTADO a los equipos cuyo ultimo latido vencio. El evento
     * se fecha en el momento del ultimo latido, que es cuando realmente se perdio
     * la comunicacion; asi el calculo de disponibilidad es mas exacto.
     *
     * @return numero de equipos marcados
     */
    public int detectarDesconexiones() {
        LocalDateTime ahora = Fechas.ahoraUtc();
        int intervalo = configuracion.intervaloSegundos();
        int umbral = configuracion.umbral();
        int marcados = 0;
        for (Dispositivo d : dispositivos.listarVigilables()) {
            if (ReglasEstado.evaluarVencimiento(d.getEstadoActual(), d.getUltimoLatido(), ahora, intervalo, umbral)
                    .isPresent()) {
                Evento e = new Evento(d, TipoEvento.DESCONEXION_INESPERADA, EstadoDispositivo.DESCONECTADO,
                        OrigenEvento.SERVIDOR, d.getUltimoLatido(),
                        "Sin latidos desde " + Fechas.iso(d.getUltimoLatido()));
                e.setFechaRegistro(ahora);
                eventos.guardar(e);
                d.setEstadoActual(EstadoDispositivo.DESCONECTADO);
                dispositivos.guardar(d);
                marcados++;
            }
        }
        if (marcados > 0) {
            LOG.info("Detector: " + marcados + " equipo(s) marcados como desconectados");
        }
        return marcados;
    }
}
