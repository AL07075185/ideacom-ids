package mx.ideacom.ids.api.dto;

import java.util.List;

/**
 * Resultado de importar dispositivos.
 *
 * @param simulado true si fue vista previa (no se guardo nada)
 * @param detalles una linea por fila: accion CREAR, ACTUALIZAR o ERROR
 * @param avisos   observaciones que no impiden importar (departamento nuevo, responsable no encontrado...)
 */
public record ResultadoImportacionDTO(boolean simulado, int filas, int creados, int actualizados, int errores,
                                      List<Detalle> detalles, List<String> avisos) {

    public record Detalle(int fila, String nombre, String accion, String mensaje) {
    }
}
