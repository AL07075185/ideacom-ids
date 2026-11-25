package mx.ideacom.ids.common.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FechasTest {

    @Test
    void isoYParseSonInversos() {
        LocalDateTime t = LocalDateTime.of(2026, 11, 20, 15, 4, 5, 123_000_000);
        String iso = Fechas.iso(t);
        assertEquals("2026-11-20T15:04:05.123Z", iso);
        assertEquals(t, Fechas.parseUtc(iso));
    }

    @Test
    void offsetSeConvierteAUtc() {
        assertEquals(LocalDateTime.of(2026, 11, 20, 18, 0),
                Fechas.parseUtc("2026-11-20T12:00:00-06:00"));
    }

    @Test
    void nuloYVacioDevuelvenNull() {
        assertNull(Fechas.parseUtc(null));
        assertNull(Fechas.parseUtc(" "));
        assertNull(Fechas.iso(null));
    }

    @Test
    void textoInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Fechas.parseUtc("ayer"));
    }
}
