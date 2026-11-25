package mx.ideacom.ids.agente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import org.junit.jupiter.api.Test;

class RecolectorTest {

    @Test
    void formatoMac() {
        assertEquals("00:1A:2B:3C:4D:5E",
                Recolector.formatoMac(new byte[]{0x00, 0x1A, 0x2B, 0x3C, 0x4D, 0x5E}));
    }

    @Test
    void recolectaDatosBasicos() {
        Recolector.InfoEquipo info = new Recolector(URI.create("http://127.0.0.1:8080/ids-api")).recolectar();
        assertNotNull(info.hostname());
        assertNotNull(info.soNombre());
        assertNotNull(info.arquitectura());
        assertTrue(info.cpuModelo().contains("núcleos"));
    }

    @Test
    void ipv6SinZona() {
        assertEquals("2806:2f0:1::10", Recolector.limpiarIpv6("2806:2f0:1::10%eth0"));
        assertEquals("2806:2f0:1::10", Recolector.limpiarIpv6("2806:2f0:1::10"));
    }
}
