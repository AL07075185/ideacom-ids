package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import mx.ideacom.ids.api.dto.IncidenciaActualizarRequest;
import mx.ideacom.ids.api.dto.IncidenciaCrearRequest;
import mx.ideacom.ids.api.dto.IncidenciaDTO;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.IncidenciaServicio;

@Path("incidencias")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class IncidenciaRecurso {

    @Inject
    private IncidenciaServicio servicio;

    @Context
    private HttpServletRequest req;

    @GET
    public List<IncidenciaDTO> listar(@QueryParam("estado") String estado) {
        return servicio.listar(Acceso.usuario(req), estado);
    }

    @POST
    public Response crear(IncidenciaCrearRequest body) {
        IncidenciaDTO creada = servicio.crear(Acceso.usuario(req), body);
        return Response.status(Response.Status.CREATED).entity(creada).build();
    }

    @PUT
    @Path("{id: \\d+}")
    public IncidenciaDTO actualizar(@PathParam("id") Integer id, IncidenciaActualizarRequest body) {
        Acceso.admin(req);
        return servicio.actualizar(id, body);
    }
}
