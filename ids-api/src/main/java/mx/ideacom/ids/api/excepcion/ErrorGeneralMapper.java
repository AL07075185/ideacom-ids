package mx.ideacom.ids.api.excepcion;

import jakarta.ejb.EJBException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.logging.Level;
import java.util.logging.Logger;
import mx.ideacom.ids.api.dto.ErrorDTO;

/**
 * Captura cualquier otra excepcion para no exponer trazas al cliente.
 * Si la excepcion envuelve una ApiException (por ejemplo dentro de una
 * transaccion), devuelve el codigo original.
 */
@Provider
public class ErrorGeneralMapper implements ExceptionMapper<Exception> {

    private static final Logger LOG = Logger.getLogger(ErrorGeneralMapper.class.getName());

    @Override
    public Response toResponse(Exception e) {
        Throwable causa = e;
        while (causa != null) {
            if (causa instanceof ApiException api) {
                return respuesta(api.getStatus(), api.getMessage());
            }
            if (causa instanceof WebApplicationException wae) {
                int status = wae.getResponse().getStatus();
                return respuesta(status, status == 404 ? "Recurso no encontrado" : wae.getMessage());
            }
            if (causa instanceof IllegalArgumentException iae) {
                return respuesta(400, iae.getMessage());
            }
            causa = (causa instanceof EJBException ejb && ejb.getCausedByException() != null)
                    ? ejb.getCausedByException() : causa.getCause();
        }
        LOG.log(Level.SEVERE, "Error no controlado", e);
        return respuesta(500, "Error interno del servidor");
    }

    private Response respuesta(int status, String mensaje) {
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDTO(status, mensaje))
                .build();
    }
}
