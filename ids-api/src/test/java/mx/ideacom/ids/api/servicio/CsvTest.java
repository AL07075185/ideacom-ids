package mx.ideacom.ids.api.servicio;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import mx.ideacom.ids.api.dto.FilaDispositivoDTO;
import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void escribeConBomYEscapaComillas() {
        String csv = Csv.escribir(List.of("nombre", "notas"),
                List.of(Arrays.asList("Cámara 1", "dijo \"hola\", luego"), Arrays.asList("NVR", null)));
        assertTrue(csv.startsWith("﻿nombre,notas\r\n"));
        assertTrue(csv.contains("Cámara 1,\"dijo \"\"hola\"\", luego\"\r\n"));
        assertTrue(csv.endsWith("NVR,\r\n"));
    }

    @Test
    void leeLoQueEscribe() {
        String csv = Csv.escribir(List.of("Nombre", "Conectado a"),
                List.of(Arrays.asList("Cámara, entrada", "Switch-01"), Arrays.asList("NVR\nprincipal", "")));
        List<Map<String, String>> filas = Csv.leer(csv);
        assertEquals(2, filas.size());
        assertEquals("Cámara, entrada", filas.get(0).get("nombre"));
        assertEquals("Switch-01", filas.get(0).get("conectadoa"));
        assertEquals("NVR\nprincipal", filas.get(1).get("nombre"));
    }

    @Test
    void aceptaPuntoYComaYOmiteRenglonesVacios() {
        List<Map<String, String>> filas = Csv.leer("nombre;ip;Ubicación\r\nRouter;192.168.1.1;Rack A\r\n;;\r\n");
        assertEquals(1, filas.size());
        assertEquals("192.168.1.1", filas.get(0).get("ip"));
        assertEquals("Rack A", filas.get(0).get("ubicacion"));
    }

    @Test
    void comillasSinCerrarOArchivoVacio() {
        assertThrows(IllegalArgumentException.class, () -> Csv.leer("nombre\n\"abierta"));
        assertThrows(IllegalArgumentException.class, () -> Csv.leer("  "));
    }

    @Test
    void filaDesdeCsv() {
        Map<String, String> f = Csv.leer("nombre,tipo,conAgente,ramMb,mac\nPC-1,PC,sí,16384,\n").get(0);
        FilaDispositivoDTO d = FilaDispositivoDTO.desdeCsv(f);
        assertEquals("PC-1", d.nombre());
        assertEquals(Boolean.TRUE, d.conAgente());
        assertEquals(Long.valueOf(16384), d.ramMb());
        assertNull(d.mac());
    }

    @Test
    void columnasYValoresCoinciden() {
        FilaDispositivoDTO d = FilaDispositivoDTO.desdeCsv(Map.of("nombre", "X"));
        assertEquals(FilaDispositivoDTO.COLUMNAS.size(), d.valores().size());
    }
}
