package mx.ideacom.ids.api.recurso;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.util.List;
import mx.ideacom.ids.api.dto.DepartamentoDTO;
import mx.ideacom.ids.api.dto.SitioDTO;
import mx.ideacom.ids.api.dto.SitioGuardarRequest;
import mx.ideacom.ids.api.dto.UsuarioDTO;
import mx.ideacom.ids.api.dto.UsuarioGuardarRequest;
import mx.ideacom.ids.api.seguridad.Acceso;
import mx.ideacom.ids.api.servicio.CatalogoServicio;

@Path("catalogos")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CatalogoRecurso {

    @Inject
    private CatalogoServicio servicio;

    @Context
    private HttpServletRequest req;

    // --- Departamentos
    @GET
    @Path("departamentos")
    public List<DepartamentoDTO> departamentos() {
        Acceso.usuario(req);
        return servicio.departamentos();
    }

    @POST
    @Path("departamentos")
    public Response crearDepartamento(DepartamentoDTO body) {
        Acceso.admin(req);
        return Response.status(Response.Status.CREATED).entity(servicio.crearDepartamento(body)).build();
    }

    @DELETE
    @Path("departamentos/{id: \\d+}")
    public Response eliminarDepartamento(@PathParam("id") Integer id) {
        Acceso.admin(req);
        servicio.eliminarDepartamento(id);
        return Response.noContent().build();
    }

    // --- Sitios
    @GET
    @Path("sitios")
    public List<SitioDTO> sitios() {
        Acceso.admin(req);
        return servicio.sitios();
    }

    @POST
    @Path("sitios")
    public Response crearSitio(SitioGuardarRequest body) {
        Acceso.admin(req);
        return Response.status(Response.Status.CREATED).entity(servicio.crearSitio(body)).build();
    }

    @PUT
    @Path("sitios/{id: \\d+}")
    public SitioDTO actualizarSitio(@PathParam("id") Integer id, SitioGuardarRequest body) {
        Acceso.admin(req);
        return servicio.actualizarSitio(id, body);
    }

    @POST
    @Path("sitios/{id: \\d+}/codigo")
    public SitioDTO regenerarCodigo(@PathParam("id") Integer id) {
        Acceso.admin(req);
        return servicio.regenerarCodigo(id);
    }

    // --- Usuarios
    @GET
    @Path("usuarios")
    public List<UsuarioDTO> usuarios() {
        Acceso.admin(req);
        return servicio.usuarios();
    }

    @POST
    @Path("usuarios")
    public Response crearUsuario(UsuarioGuardarRequest body) {
        Acceso.admin(req);
        return Response.status(Response.Status.CREATED).entity(servicio.crearUsuario(body)).build();
    }

    @PUT
    @Path("usuarios/{id: \\d+}")
    public UsuarioDTO actualizarUsuario(@PathParam("id") Integer id, UsuarioGuardarRequest body) {
        return servicio.actualizarUsuario(Acceso.admin(req), id, body);
    }
}
