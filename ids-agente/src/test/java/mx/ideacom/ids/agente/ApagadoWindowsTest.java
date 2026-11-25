package mx.ideacom.ids.agente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import org.junit.jupiter.api.Test;

class ApagadoWindowsTest {

    private static String evento(String id) {
        return "<Event xmlns='http://schemas.microsoft.com/win/2004/08/events/event'><System>"
                + "<Provider Name='EventLog'/><EventID Qualifiers='32768'>" + id + "</EventID>"
                + "<TimeCreated SystemTime='2026-11-17T18:01:22.1234567Z'/></System></Event>";
    }

    @Test
    void apagadoNormal6006() {
        ApagadoWindows.Apagado a = ApagadoWindows.interpretar(evento("6006")).orElseThrow();
        assertEquals(ApagadoWindows.Tipo.NORMAL, a.tipo());
        assertEquals(Instant.parse("2026-11-17T18:01:22.1234567Z"), a.fecha());
    }

    @Test
    void reinicioIniciadoPorUsuario1074() {
        assertEquals(ApagadoWindows.Tipo.NORMAL, ApagadoWindows.interpretar(evento("1074")).orElseThrow().tipo());
    }

    @Test
    void apagadoInesperado6008() {
        assertEquals(ApagadoWindows.Tipo.INESPERADO, ApagadoWindows.interpretar(evento("6008")).orElseThrow().tipo());
    }

    @Test
    void sinEventosOSalidaInvalida() {
        assertTrue(ApagadoWindows.interpretar("").isEmpty());
        assertTrue(ApagadoWindows.interpretar(null).isEmpty());
        assertTrue(ApagadoWindows.interpretar("Error: acceso denegado").isEmpty());
    }
}
