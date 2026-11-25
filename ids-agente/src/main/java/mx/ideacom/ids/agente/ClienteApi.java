package mx.ideacom.ids.agente;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import mx.ideacom.ids.common.dto.LatidoRequest;
import mx.ideacom.ids.common.dto.LatidoResponse;
import mx.ideacom.ids.common.dto.RegistroAgenteRequest;
import mx.ideacom.ids.common.dto.RegistroAgenteResponse;

/** Cliente HTTP de la API (java.net.http, incluido en Java 11+). */
public final class ClienteApi {

    /** El servidor rechazo el token (401): hay que registrarse de nuevo. */
    public static final class TokenInvalidoException extends IOException {
        private static final long serialVersionUID = 1L;

        public TokenInvalidoException(String m) {
            super(m);
        }
    }

    /** El servidor nego el acceso (403): codigo de alta invalido o equipo rechazado. */
    public static final class AccesoDenegadoException extends IOException {
        private static final long serialVersionUID = 1L;

        public AccesoDenegadoException(String m) {
            super(m);
        }
    }

    /** El servidor respondio con un error distinto (400, 500...). */
    public static final class RespuestaErrorException extends IOException {
        private static final long serialVersionUID = 1L;

        public RespuestaErrorException(String m) {
            super(m);
        }
    }

    private final URI base;
    private final HttpClient http;
    private final Duration timeout;

    public ClienteApi(URI base, int timeoutMs) {
        this.base = base;
        this.timeout = Duration.ofMillis(timeoutMs);
        this.http = HttpClient.newBuilder().connectTimeout(timeout).build();
    }

    public RegistroAgenteResponse registrar(RegistroAgenteRequest req) throws IOException {
        Map<String, Object> m = Json.leerObjeto(post("/agentes/registro", null, Json.escribir(req), timeout));
        return new RegistroAgenteResponse(Json.entero(m, "dispositivoId"), Json.texto(m, "token"),
                Json.entero(m, "intervaloSegundos"), Json.texto(m, "estadoRegistro"));
    }

    public LatidoResponse latido(String token, LatidoRequest req) throws IOException {
        Map<String, Object> m = Json.leerObjeto(post("/agentes/latido", token, Json.escribir(req), timeout));
        return new LatidoResponse(Json.entero(m, "intervaloSegundos"), Json.texto(m, "estadoRegistro"));
    }

    public void eventos(String token, List<EventoAgenteDTO> eventos, Duration limite) throws IOException {
        post("/agentes/eventos", token, Json.escribir(eventos), limite == null ? timeout : limite);
    }

    private String post(String ruta, String token, String cuerpo, Duration limite) throws IOException {
        HttpRequest.Builder b = HttpRequest.newBuilder(URI.create(base + ruta))
                .timeout(limite)
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("User-Agent", "ids-agente/" + ConfigAgente.VERSION)
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo, StandardCharsets.UTF_8));
        if (token != null) {
            b.header("Authorization", "Bearer " + token);
        }
        HttpResponse<String> r;
        try {
            r = http.send(b.build(), HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IOException("Peticion interrumpida", e);
        }
        int status = r.statusCode();
        if (status == 401) {
            throw new TokenInvalidoException(mensaje(r.body()));
        }
        if (status == 403) {
            throw new AccesoDenegadoException(mensaje(r.body()));
        }
        if (status >= 400) {
            throw new RespuestaErrorException("HTTP " + status + ": " + mensaje(r.body()));
        }
        return r.body() == null || r.body().isBlank() ? "{}" : r.body();
    }

    private static String mensaje(String cuerpo) {
        try {
            String m = Json.texto(Json.leerObjeto(cuerpo), "mensaje");
            return m != null ? m : cuerpo;
        } catch (RuntimeException e) {
            return cuerpo;
        }
    }
}
