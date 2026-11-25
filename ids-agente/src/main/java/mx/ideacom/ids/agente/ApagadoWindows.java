package mx.ideacom.ids.agente;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Consulta el registro de eventos de Windows para saber como termino la sesion
 * anterior del equipo. Es necesario porque, al apagar Windows, el Programador de
 * tareas termina el agente sin darle tiempo de avisar.
 *
 * <ul>
 *   <li>Evento 6006 (o 1074): apagado o reinicio normal.</li>
 *   <li>Evento 6008: el apagado anterior fue inesperado (corte de energia, apagado forzado).</li>
 * </ul>
 */
public final class ApagadoWindows {

    private static final Logger LOG = Logger.getLogger(ApagadoWindows.class.getName());
    private static final Pattern EVENT_ID = Pattern.compile("<EventID[^>]*>(\\d+)</EventID>");
    private static final Pattern FECHA = Pattern.compile("SystemTime=['\"]([^'\"]+)['\"]");

    /** Tipo de cierre de la sesion anterior. */
    public enum Tipo { NORMAL, INESPERADO }

    /** Ultimo apagado registrado por Windows. Para INESPERADO, la fecha es la del arranque siguiente. */
    public record Apagado(Tipo tipo, Instant fecha) {
    }

    private ApagadoWindows() {
    }

    public static boolean esWindows() {
        return System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
    }

    /** Ultimo evento de apagado del registro "System", o vacio si no se pudo consultar. */
    public static Optional<Apagado> ultimoApagado() {
        if (!esWindows()) {
            return Optional.empty();
        }
        List<String> comando = List.of("wevtutil", "qe", "System",
                "/q:*[System[(EventID=6006 or EventID=6008 or EventID=1074)]]",
                "/c:1", "/rd:true", "/f:xml");
        try {
            Process p = new ProcessBuilder(comando).redirectErrorStream(true).start();
            String salida;
            try (InputStream in = p.getInputStream()) {
                salida = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
            if (!p.waitFor(10, TimeUnit.SECONDS)) {
                p.destroyForcibly();
            }
            return interpretar(salida);
        } catch (IOException e) {
            LOG.fine("No se pudo consultar el registro de eventos: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return Optional.empty();
    }

    /** Interpreta la salida XML de wevtutil (separado para poder probarlo). */
    static Optional<Apagado> interpretar(String xml) {
        if (xml == null) {
            return Optional.empty();
        }
        Matcher id = EVENT_ID.matcher(xml);
        Matcher fecha = FECHA.matcher(xml);
        if (!id.find() || !fecha.find()) {
            return Optional.empty();
        }
        try {
            Instant cuando = Instant.parse(fecha.group(1));
            Tipo tipo = "6008".equals(id.group(1)) ? Tipo.INESPERADO : Tipo.NORMAL;
            return Optional.of(new Apagado(tipo, cuando));
        } catch (DateTimeParseException e) {
            return Optional.empty();
        }
    }
}
