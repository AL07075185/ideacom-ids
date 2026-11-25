package mx.ideacom.ids.api.config;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Activa JAX-RS. Todas las rutas quedan bajo /ids-api/api/v1. */
@ApplicationPath("/api/v1")
public class RestConfig extends Application {
}
