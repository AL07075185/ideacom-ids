package mx.ideacom.ids.api.web;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Middleware de logs: registra metodo, ruta, codigo de respuesta y duracion de
 * cada peticion a la API. No registra cuerpos ni tokens.
 * Los latidos exitosos se registran en nivel FINE para no saturar el log.
 */
@WebFilter(urlPatterns = {"/api/*", "/reportes/*"})
public class FiltroLog implements Filter {

    private static final Logger LOG = Logger.getLogger("mx.ideacom.ids.acceso");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        long inicio = System.nanoTime();
        try {
            chain.doFilter(request, response);
        } finally {
            long ms = (System.nanoTime() - inicio) / 1_000_000;
            int status = res.getStatus();
            String uri = req.getRequestURI();
            Level nivel = status >= 500 ? Level.WARNING
                    : (status < 400 && uri.endsWith("/agentes/latido")) ? Level.FINE : Level.INFO;
            if (LOG.isLoggable(nivel)) {
                LOG.log(nivel, String.format("%s %s -> %d (%d ms) desde %s",
                        req.getMethod(), uri, status, ms, ipCliente(req)));
            }
        }
    }

    /** Detras de Nginx la IP real viene en X-Forwarded-For. */
    private static String ipCliente(HttpServletRequest req) {
        String xff = req.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank()) ? xff.split(",")[0].trim() : req.getRemoteAddr();
    }
}
