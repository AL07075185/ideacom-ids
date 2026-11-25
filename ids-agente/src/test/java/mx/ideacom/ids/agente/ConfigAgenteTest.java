package mx.ideacom.ids.agente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigAgenteTest {

    /** Carpeta temporal del sistema: absoluta en Windows (C:\...) y en Linux (/tmp). */
    private static final Path BASE = Path.of(System.getProperty("java.io.tmpdir")).toAbsolutePath();

    private static Properties base() {
        Properties p = new Properties();
        p.setProperty("api.url", "http://localhost:8080/ids-api/api/v1/");
        p.setProperty("codigo.alta", "IDS-DEMO-2026");
        return p;
    }

    @Test
    void valoresPorDefecto() {
        ConfigAgente c = ConfigAgente.desde(base(), BASE);
        assertEquals("http://localhost:8080/ids-api/api/v1", c.apiUrl().toString());
        assertEquals(60, c.intervaloSegundos());
        assertEquals(2, c.hostsInternet().size());
        assertTrue(c.datosDir().isAbsolute());
    }

    @Test
    void produccionExigeHttps() {
        Properties p = base();
        p.setProperty("perfil", "produccion");
        assertThrows(IllegalArgumentException.class, () -> ConfigAgente.desde(p, BASE));
        p.setProperty("api.url", "https://ids.ideacom.mx/ids-api/api/v1");
        assertTrue(ConfigAgente.desde(p, BASE).esProduccion());
    }

    @Test
    void faltaCodigoAlta() {
        Properties p = base();
        p.remove("codigo.alta");
        assertThrows(IllegalArgumentException.class, () -> ConfigAgente.desde(p, BASE));
    }

    @Test
    void intervaloMinimoDiezSegundos() {
        Properties p = base();
        p.setProperty("intervalo.segundos", "2");
        assertEquals(10, ConfigAgente.desde(p, BASE).intervaloSegundos());
    }
}
