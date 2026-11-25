package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import mx.ideacom.ids.api.dto.ResumenDTO;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.DashboardServicio;

@Path("dashboard")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class DashboardRecurso {

    @Inject
    private DashboardServicio servicio;

    @Context
    private HttpServletRequest req;

    @GET
    @Path("resumen")
    public ResumenDTO resumen() {
        return servicio.resumen(Acceso.usuario(req));
    }
}
