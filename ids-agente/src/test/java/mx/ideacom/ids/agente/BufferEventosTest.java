package mx.ideacom.ids.agente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import org.junit.jupiter.api.Test;

class BufferEventosTest {

    @Test
    void agregaLeeYQuita() throws Exception {
        Path dir = Files.createTempDirectory("ids-buffer");
        BufferEventos b = new BufferEventos(dir);
        assertTrue(b.vacio());
        b.agregar(new EventoAgenteDTO("ARRANQUE", "2026-11-20T10:00:00Z", null, "uno"));
        b.agregar(new EventoAgenteDTO("APAGADO", "2026-11-20T11:00:00Z", null, "dos"));
        assertEquals(2, b.leer().size());

        // Persiste entre instancias (reinicio del agente)
        BufferEventos otra = new BufferEventos(dir);
        assertEquals("dos", otra.leer().get(1).detalle());

        otra.quitarPrimeros(1);
        assertEquals(1, otra.leer().size());
        assertEquals("APAGADO", otra.leer().get(0).tipo());
        otra.quitarPrimeros(5);
        assertTrue(otra.vacio());
    }

    @Test
    void estadoLocalGuardaToken() throws Exception {
        Path dir = Files.createTempDirectory("ids-estado");
        EstadoLocal e = new EstadoLocal(dir);
        e.guardar(12, "tok");
        EstadoLocal leido = new EstadoLocal(dir);
        assertEquals(Integer.valueOf(12), leido.dispositivoId());
        assertEquals("tok", leido.token());
        leido.borrarToken();
        assertEquals(false, new EstadoLocal(dir).tieneToken());
    }

    @Test
    void estadoLocalGuardaUltimoContacto() throws Exception {
        Path dir = Files.createTempDirectory("ids-contacto");
        EstadoLocal e = new EstadoLocal(dir);
        e.guardar(3, "tok");
        java.time.Instant t = java.time.Instant.parse("2026-11-17T18:00:00Z");
        e.registrarContacto(t);
        assertEquals(t, new EstadoLocal(dir).ultimoContacto());
    }
}
