package mx.ideacom.ids.api.dto;

import java.util.List;

/** Resultado de restaurar un respaldo: conteo por seccion y observaciones. */
public record ResultadoRestauracionDTO(List<Seccion> secciones, List<String> avisos) {

    public record Seccion(String nombre, int creados, int actualizados, int omitidos) {
    }
}
