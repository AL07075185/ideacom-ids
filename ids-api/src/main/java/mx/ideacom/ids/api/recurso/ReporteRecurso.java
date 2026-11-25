package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.util.Map;
import mx.ideacom.ids.api.reporte.TicketServicio;
import mx.ideacom.ids.api.seguridad.Acceso;

/** Emite el ticket de un solo uso para abrir el reporte JSP. */
@Path("reportes")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class ReporteRecurso {

    @Inject
    private TicketServicio tickets;

    @Context
    private HttpServletRequest req;

    @POST
    @Path("ticket")
    public Map<String, String> ticket() {
        return Map.of("ticket", tickets.crear(Acceso.usuario(req)));
    }
}
