package mx.ideacom.ids.api.reporte;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import mx.ideacom.ids.api.seguridad.Sujeto;
import org.junit.jupiter.api.Test;

class ReporteServicioTest {

    @Test
    void duracionLegible() {
        assertEquals("0 min", ReporteServicio.duracion(0));
        assertEquals("45 min", ReporteServicio.duracion(45));
        assertEquals("2 h", ReporteServicio.duracion(120));
        assertEquals("1 h 5 min", ReporteServicio.duracion(65));
    }

    @Test
    void ticketDeUnSoloUso() {
        TicketServicio t = new TicketServicio();
        Sujeto s = new Sujeto(Sujeto.Tipo.USUARIO, 1, "Admin", true);
        String ticket = t.crear(s);
        assertEquals(s, t.canjear(ticket).orElseThrow());
        assertTrue(t.canjear(ticket).isEmpty());
        assertTrue(t.canjear("inventado").isEmpty());
    }
}
