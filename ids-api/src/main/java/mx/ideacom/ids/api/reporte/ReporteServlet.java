package mx.ideacom.ids.api.reporte;

import jakarta.inject.Inject;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.Optional;
import java.util.Set;
import mx.ideacom.ids.api.seguridad.Sujeto;

/**
 * Controlador (MVC del lado del servidor) del reporte de disponibilidad.
 * <ol>
 *   <li>El panel pide un ticket de un solo uso y abre esta URL con ?ticket=...</li>
 *   <li>El servlet canjea el ticket, guarda al usuario en la sesion HTTP (15 min) y
 *       redirige a la misma URL sin el ticket (asi se puede recargar o imprimir).</li>
 *   <li>Arma el modelo con ReporteServicio y lo envia a la vista JSP.</li>
 * </ol>
 */
@WebServlet(urlPatterns = "/reportes/disponibilidad")
public class ReporteServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    static final String ATRIBUTO_SESION = "ids.reporte.sujeto";
    private static final Set<Integer> HORAS_VALIDAS = Set.of(24, 168, 720);
    private static final String VISTA = "/WEB-INF/vistas/reporte-disponibilidad.jsp";
    private static final String VISTA_ERROR = "/WEB-INF/vistas/reporte-error.jsp";

    @Inject
    private transient TicketServicio tickets;

    @Inject
    private transient ReporteServicio servicio;

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        req.setCharacterEncoding("UTF-8");
        String ticket = req.getParameter("ticket");
        if (ticket != null) {
            Optional<Sujeto> sujeto = tickets.canjear(ticket);
            if (sujeto.isEmpty()) {
                error(req, resp, "El enlace del reporte expiró. Vuelve a abrirlo desde el panel.");
                return;
            }
            HttpSession sesion = req.getSession(true);
            sesion.setMaxInactiveInterval(15 * 60);
            sesion.setAttribute(ATRIBUTO_SESION, sujeto.get());
            resp.sendRedirect(req.getContextPath() + "/reportes/disponibilidad?" + parametros(req));
            return;
        }

        HttpSession sesion = req.getSession(false);
        Object guardado = sesion == null ? null : sesion.getAttribute(ATRIBUTO_SESION);
        if (!(guardado instanceof Sujeto sujeto)) {
            error(req, resp, "La sesión del reporte terminó. Vuelve a abrirlo desde el panel.");
            return;
        }

        int horas = entero(req.getParameter("horas"), 24);
        if (!HORAS_VALIDAS.contains(horas)) {
            horas = 24;
        }
        Integer idSitio = req.getParameter("sitio") == null || req.getParameter("sitio").isBlank()
                ? null : entero(req.getParameter("sitio"), -1);
        req.setAttribute("reporte", servicio.generar(sujeto, idSitio != null && idSitio < 0 ? null : idSitio, horas,
                zona(req.getParameter("tz"))));
        req.getRequestDispatcher(VISTA).forward(req, resp);
    }

    private static void error(HttpServletRequest req, HttpServletResponse resp, String mensaje)
            throws ServletException, IOException {
        resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        req.setAttribute("mensaje", mensaje);
        req.getRequestDispatcher(VISTA_ERROR).forward(req, resp);
    }

    /** Conserva horas, sitio y zona horaria al quitar el ticket de la URL. */
    private static String parametros(HttpServletRequest req) {
        StringBuilder sb = new StringBuilder();
        for (String p : new String[]{"horas", "sitio", "tz"}) {
            String v = req.getParameter(p);
            if (v != null && !v.isBlank()) {
                if (sb.length() > 0) {
                    sb.append('&');
                }
                sb.append(p).append('=').append(URLEncoder.encode(v, StandardCharsets.UTF_8));
            }
        }
        return sb.toString();
    }

    private static int entero(String texto, int defecto) {
        try {
            return texto == null ? defecto : Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return defecto;
        }
    }

    private static ZoneId zona(String tz) {
        try {
            return tz == null || tz.isBlank() ? ZoneId.of("UTC") : ZoneId.of(tz);
        } catch (DateTimeException e) {
            return ZoneId.of("UTC");
        }
    }
}
