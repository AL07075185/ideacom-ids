package mx.ideacom.ids.api.seguridad;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Optional;
import java.util.Set;
import mx.ideacom.ids.api.dto.ErrorDTO;
import mx.ideacom.ids.api.servicio.AuthServicio;

/**
 * Autentica cada peticion REST con un token Bearer:
 * <ul>
 *   <li>Rutas /agentes/** (salvo registro): token de agente.</li>
 *   <li>Resto: token de sesion del panel.</li>
 * </ul>
 * El sujeto autenticado queda en el atributo {@link Acceso#ATRIBUTO}.
 */
@Provider
@Priority(Priorities.AUTHENTICATION)
public class FiltroAutenticacion implements ContainerRequestFilter {

    private static final Set<String> RUTAS_PUBLICAS = Set.of("auth/login", "agentes/registro");

    @Inject
    private AuthServicio authServicio;

    @Override
    public void filter(ContainerRequestContext ctx) {
        String ruta = normalizar(ctx.getUriInfo().getPath());
        if ("OPTIONS".equals(ctx.getMethod()) || RUTAS_PUBLICAS.contains(ruta)) {
            return;
        }
        String token = extraerToken(ctx.getHeaderString(HttpHeaders.AUTHORIZATION));
        if (token == null) {
            abortar(ctx, "Falta el encabezado Authorization: Bearer <token>");
            return;
        }
        Optional<Sujeto> sujeto = ruta.startsWith("agentes/")
                ? authServicio.autenticarAgente(token)
                : authServicio.autenticarSesion(token);
        if (sujeto.isEmpty()) {
            abortar(ctx, "Token inválido o expirado");
            return;
        }
        // En el contenedor de servlets, las propiedades de la peticion JAX-RS son
        // los atributos del HttpServletRequest: los recursos las leen con Acceso.
        ctx.setProperty(Acceso.ATRIBUTO, sujeto.get());
    }

    private static String normalizar(String ruta) {
        if (ruta == null) {
            return "";
        }
        String r = ruta.startsWith("/") ? ruta.substring(1) : ruta;
        return r.endsWith("/") ? r.substring(0, r.length() - 1) : r;
    }

    static String extraerToken(String header) {
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String t = header.substring(7).trim();
        return t.isEmpty() ? null : t;
    }

    private static void abortar(ContainerRequestContext ctx, String mensaje) {
        ctx.abortWith(Response.status(Response.Status.UNAUTHORIZED)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDTO(401, mensaje))
                .build());
    }
}
