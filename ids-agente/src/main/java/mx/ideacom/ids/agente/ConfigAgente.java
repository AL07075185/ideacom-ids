package mx.ideacom.ids.agente;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 * Configuracion del agente, leida de agente.properties.
 *
 * <pre>
 * api.url=http://localhost:8080/ids-api/api/v1
 * codigo.alta=IDS-DEMO-2026
 * perfil=local                 # local | produccion (produccion exige https)
 * intervalo.segundos=60        # inicial; el servidor puede ajustarlo
 * timeout.ms=5000
 * internet.hosts=1.1.1.1:443,8.8.8.8:53
 * datos.dir=.                  # donde guarda token, eventos pendientes y log
 * </pre>
 */
public final class ConfigAgente {

    public static final String VERSION = "1.0.0";

    /** Host y puerto para probar la salida a internet. */
    public record Destino(String host, int puerto) {
    }

    private final URI apiUrl;
    private final String codigoAlta;
    private final String perfil;
    private final int intervaloSegundos;
    private final int timeoutMs;
    private final List<Destino> hostsInternet;
    private final Path datosDir;

    private ConfigAgente(Properties p, Path base) {
        String url = requerido(p, "api.url");
        this.apiUrl = URI.create(url.endsWith("/") ? url.substring(0, url.length() - 1) : url);
        this.codigoAlta = requerido(p, "codigo.alta");
        this.perfil = p.getProperty("perfil", "local").trim().toLowerCase(Locale.ROOT);
        this.intervaloSegundos = Math.max(10, entero(p, "intervalo.segundos", 60));
        this.timeoutMs = Math.max(1000, entero(p, "timeout.ms", 5000));
        this.hostsInternet = destinos(p.getProperty("internet.hosts", "1.1.1.1:443,8.8.8.8:53"));
        String dir = p.getProperty("datos.dir", ".").trim();
        Path d = Path.of(dir);
        this.datosDir = d.isAbsolute() ? d : base.resolve(d).normalize();

        String esquema = apiUrl.getScheme() == null ? "" : apiUrl.getScheme().toLowerCase(Locale.ROOT);
        if (!esquema.equals("http") && !esquema.equals("https")) {
            throw new IllegalArgumentException("api.url debe empezar con http:// o https://");
        }
        if (esProduccion() && !esquema.equals("https")) {
            throw new IllegalArgumentException("En perfil produccion api.url debe usar https://");
        }
    }

    public static ConfigAgente cargar(Path archivo) throws IOException {
        Properties p = new Properties();
        try (InputStream in = Files.newInputStream(archivo)) {
            p.load(in);
        }
        Path base = archivo.toAbsolutePath().getParent();
        return new ConfigAgente(p, base == null ? Path.of(".") : base);
    }

    static ConfigAgente desde(Properties p, Path base) {
        return new ConfigAgente(p, base);
    }

    public URI apiUrl() {
        return apiUrl;
    }

    public String codigoAlta() {
        return codigoAlta;
    }

    public boolean esProduccion() {
        return "produccion".equals(perfil);
    }

    public int intervaloSegundos() {
        return intervaloSegundos;
    }

    public int timeoutMs() {
        return timeoutMs;
    }

    public List<Destino> hostsInternet() {
        return hostsInternet;
    }

    public Path datosDir() {
        return datosDir;
    }

    private static String requerido(Properties p, String clave) {
        String v = p.getProperty(clave);
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Falta la propiedad '" + clave + "' en agente.properties");
        }
        return v.trim();
    }

    private static int entero(Properties p, String clave, int defecto) {
        String v = p.getProperty(clave);
        try {
            return v == null ? defecto : Integer.parseInt(v.trim());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("'" + clave + "' debe ser un numero entero");
        }
    }

    private static List<Destino> destinos(String texto) {
        List<Destino> lista = new ArrayList<>();
        for (String parte : texto.split(",")) {
            String t = parte.trim();
            if (t.isEmpty()) {
                continue;
            }
            int dos = t.lastIndexOf(':');
            if (dos <= 0) {
                throw new IllegalArgumentException("internet.hosts debe tener formato host:puerto");
            }
            lista.add(new Destino(t.substring(0, dos), Integer.parseInt(t.substring(dos + 1))));
        }
        return List.copyOf(lista);
    }
}
