package mx.ideacom.ids.agente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import mx.ideacom.ids.common.dto.LatidoRequest;
import org.junit.jupiter.api.Test;

class JsonTest {

    @Test
    void escribeRecordOmitiendoNulos() {
        String json = Json.escribir(new LatidoRequest("10.0.0.5", null, true, null));
        assertEquals("{\"ip\":\"10.0.0.5\",\"internetOk\":true}", json);
    }

    @Test
    void escribeListaYEscapaTexto() {
        String json = Json.escribir(List.of(new EventoAgenteDTO("APAGADO", "2026-11-20T10:00:00Z", null, "dijo \"adios\"")));
        assertEquals("[{\"tipo\":\"APAGADO\",\"fechaHora\":\"2026-11-20T10:00:00Z\",\"detalle\":\"dijo \\\"adios\\\"\"}]", json);
    }

    @Test
    void leeObjetoConTipos() {
        Map<String, Object> m = Json.leerObjeto(
                "{\"dispositivoId\": 7, \"token\":\"abc\", \"ok\":true, \"x\":null, \"r\":1.5, \"l\":[1,2], \"o\":{\"a\":\"\\u00e9\"}}");
        assertEquals(Integer.valueOf(7), Json.entero(m, "dispositivoId"));
        assertEquals("abc", Json.texto(m, "token"));
        assertEquals(Boolean.TRUE, m.get("ok"));
        assertNull(m.get("x"));
        assertEquals(1.5, m.get("r"));
        assertEquals("\u00e9", ((Map<?, ?>) m.get("o")).get("a"));
    }

    @Test
    void jsonInvalidoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> Json.leerObjeto("{\"a\":"));
        assertThrows(IllegalArgumentException.class, () -> Json.leerObjeto("[1,2]"));
    }
}
