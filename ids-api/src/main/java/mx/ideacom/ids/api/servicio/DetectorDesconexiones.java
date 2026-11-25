package mx.ideacom.ids.api.servicio;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Tareas programadas del servidor (temporizadores EJB no persistentes).
 * <ul>
 *   <li>Cada 30 s: detecta equipos que dejaron de enviar latidos.</li>
 *   <li>Cada hora: elimina sesiones expiradas.</li>
 * </ul>
 */
@Singleton
@Startup
@TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
public class DetectorDesconexiones {

    private static final Logger LOG = Logger.getLogger(DetectorDesconexiones.class.getName());

    @Inject
    private MonitorServicio monitor;

    @Inject
    private AuthServicio auth;

    @Schedule(second = "*/30", minute = "*", hour = "*", persistent = false)
    public void revisarLatidos() {
        try {
            monitor.detectarDesconexiones();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Fallo la revision de latidos", e);
        }
    }

    @Schedule(minute = "7", hour = "*", persistent = false)
    public void limpiarSesiones() {
        try {
            auth.limpiarSesionesExpiradas();
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Fallo la limpieza de sesiones", e);
        }
    }
}
