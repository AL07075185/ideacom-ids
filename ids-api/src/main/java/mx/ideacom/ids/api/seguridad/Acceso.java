package mx.ideacom.ids.api.seguridad;

import jakarta.servlet.http.HttpServletRequest;
import mx.ideacom.ids.api.excepcion.ApiException;

/** Ayudas para que los recursos REST obtengan y validen al sujeto autenticado. */
public final class Acceso {

    /** Atributo de la peticion donde FiltroAutenticacion deja al sujeto. */
    public static final String ATRIBUTO = "ids.sujeto";

    private Acceso() {
    }

    public static Sujeto sujeto(HttpServletRequest req) {
        Object s = req.getAttribute(ATRIBUTO);
        if (!(s instanceof Sujeto sujeto)) {
            throw ApiException.noAutenticado("Se requiere autenticación");
        }
        return sujeto;
    }

    public static Sujeto usuario(HttpServletRequest req) {
        Sujeto s = sujeto(req);
        if (!s.esUsuario()) {
            throw ApiException.prohibido("Solo usuarios del panel");
        }
        return s;
    }

    public static Sujeto admin(HttpServletRequest req) {
        Sujeto s = usuario(req);
        if (!s.admin()) {
            throw ApiException.prohibido("Se requiere rol de administrador");
        }
        return s;
    }

    public static Sujeto agente(HttpServletRequest req) {
        Sujeto s = sujeto(req);
        if (!s.esAgente()) {
            throw ApiException.prohibido("Solo agentes");
        }
        return s;
    }
}
