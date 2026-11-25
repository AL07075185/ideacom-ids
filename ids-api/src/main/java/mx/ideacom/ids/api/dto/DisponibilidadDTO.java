package mx.ideacom.ids.api.dto;

/** porcentaje es null cuando no hubo tiempo medible (equipo apagado o nuevo). */
public record DisponibilidadDTO(Integer idDispositivo, int horas, Double porcentaje) {
}
