package mx.ideacom.ids.api.dto;

/** Campos nulos = no cambiar. idAsignadaA = 0 quita la asignacion. */
public record IncidenciaActualizarRequest(String estado, String prioridad, Integer idAsignadaA) {
}
