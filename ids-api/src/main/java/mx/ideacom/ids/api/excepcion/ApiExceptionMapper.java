package mx.ideacom.ids.api.excepcion;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import mx.ideacom.ids.api.dto.ErrorDTO;

/** Convierte ApiException en una respuesta JSON {status, mensaje}. */
@Provider
public class ApiExceptionMapper implements ExceptionMapper<ApiException> {

    @Override
    public Response toResponse(ApiException e) {
        return Response.status(e.getStatus())
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDTO(e.getStatus(), e.getMessage()))
                .build();
    }
}
