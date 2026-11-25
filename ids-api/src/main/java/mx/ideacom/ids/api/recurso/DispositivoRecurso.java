package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
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
import mx.ideacom.ids.api.dto.DisponibilidadDTO;
import mx.ideacom.ids.api.dto.DispositivoDTO;
import mx.ideacom.ids.api.dto.DispositivoGuardarRequest;
import mx.ideacom.ids.api.dto.EventoDTO;
import mx.ideacom.ids.api.dto.MapaDTO;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.DispositivoServicio;

@Path("dispositivos")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class DispositivoRecurso {

    @Inject
    private DispositivoServicio servicio;

    @Context
    private HttpServletRequest req;

    @GET
    public List<DispositivoDTO> listar(@QueryParam("registro") String registro) {
        return servicio.listar(Acceso.usuario(req), registro);
    }

    @GET
    @Path("mapa")
    public MapaDTO mapa() {
        return servicio.mapa(Acceso.usuario(req));
    }

    @GET
    @Path("{id: \\d+}")
    public DispositivoDTO obtener(@PathParam("id") Integer id) {
        return servicio.obtener(Acceso.usuario(req), id);
    }

    @GET
    @Path("{id: \\d+}/eventos")
    public List<EventoDTO> eventos(@PathParam("id") Integer id,
                                   @QueryParam("limite") @DefaultValue("50") int limite) {
        return servicio.eventos(Acceso.usuario(req), id, limite);
    }

    @GET
    @Path("{id: \\d+}/disponibilidad")
    public DisponibilidadDTO disponibilidad(@PathParam("id") Integer id,
                                            @QueryParam("horas") @DefaultValue("24") int horas) {
        return servicio.disponibilidad(Acceso.usuario(req), id, horas);
    }

    @POST
    public Response crearManual(DispositivoGuardarRequest body) {
        Acceso.admin(req);
        return Response.status(Response.Status.CREATED).entity(servicio.crearManual(body)).build();
    }

    @PUT
    @Path("{id: \\d+}")
    public DispositivoDTO actualizar(@PathParam("id") Integer id, DispositivoGuardarRequest body) {
        Acceso.admin(req);
        return servicio.actualizar(id, body);
    }

    @POST
    @Path("{id: \\d+}/aprobar")
    public DispositivoDTO aprobar(@PathParam("id") Integer id) {
        Acceso.admin(req);
        return servicio.aprobar(id);
    }

    @POST
    @Path("{id: \\d+}/rechazar")
    public DispositivoDTO rechazar(@PathParam("id") Integer id) {
        Acceso.admin(req);
        return servicio.rechazar(id);
    }

    @DELETE
    @Path("{id: \\d+}")
    public Response eliminar(@PathParam("id") Integer id) {
        Acceso.admin(req);
        servicio.eliminar(id);
        return Response.noContent().build();
    }
}
