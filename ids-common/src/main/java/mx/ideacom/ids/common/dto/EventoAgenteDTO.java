package mx.ideacom.ids.common.dto;

/**
 * Evento reportado por el agente. Las fechas viajan como texto ISO-8601 en UTC
 * (por ejemplo 2026-11-20T15:04:05.123Z) para no depender de librerias de fechas.
 */
public record EventoAgenteDTO(
        String tipo,
        String fechaHora,
        String fechaFin,
        String detalle) {
}
