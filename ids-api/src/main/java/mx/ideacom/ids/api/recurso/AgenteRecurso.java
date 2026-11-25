package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.AgenteServicio;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import mx.ideacom.ids.common.dto.LatidoRequest;
import mx.ideacom.ids.common.dto.LatidoResponse;
import mx.ideacom.ids.common.dto.RegistroAgenteRequest;
import mx.ideacom.ids.common.dto.RegistroAgenteResponse;

/** Endpoints que consume el agente instalado en cada equipo. */
@Path("agentes")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgenteRecurso {

    @Inject
    private AgenteServicio servicio;

    @Context
    private HttpServletRequest req;

    /** Publico: se protege con el codigo de alta del sitio. */
    @POST
    @Path("registro")
    public RegistroAgenteResponse registro(RegistroAgenteRequest body) {
        return servicio.registrar(body);
    }

    @POST
    @Path("latido")
    public LatidoResponse latido(LatidoRequest body) {
        return servicio.latido(Acceso.agente(req).id(), body);
    }

    @POST
    @Path("eventos")
    public Response eventos(List<EventoAgenteDTO> body) {
        servicio.eventos(Acceso.agente(req).id(), body);
        return Response.noContent().build();
    }
}
