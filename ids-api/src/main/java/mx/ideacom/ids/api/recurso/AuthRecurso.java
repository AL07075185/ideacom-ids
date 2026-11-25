package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import mx.ideacom.ids.api.dto.LoginRequest;
import mx.ideacom.ids.api.dto.LoginResponse;
import mx.ideacom.ids.api.dto.UsuarioDTO;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.AuthServicio;

@Path("auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthRecurso {

    @Inject
    private AuthServicio servicio;

    @Context
    private HttpServletRequest req;

    @POST
    @Path("login")
    public LoginResponse login(LoginRequest body) {
        return servicio.login(body);
    }

    @POST
    @Path("logout")
    public Response logout(@HeaderParam(HttpHeaders.AUTHORIZATION) String authorization) {
        Acceso.usuario(req);
        servicio.logout(authorization.substring(7).trim());
        return Response.noContent().build();
    }

    @GET
    @Path("yo")
    public UsuarioDTO yo() {
        return servicio.yo(Acceso.usuario(req));
    }
}
