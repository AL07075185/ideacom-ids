package mx.ideacom.ids.api.dto;

import java.util.List;
import java.util.Map;

/** Datos del dashboard. */
public record ResumenDTO(
        long totalDispositivos,
        Map<String, Long> porEstado,
        long pendientes,
        long incidenciasAbiertas,
        Double disponibilidad24h,
        List<EventoDTO> ultimosEventos) {
}
