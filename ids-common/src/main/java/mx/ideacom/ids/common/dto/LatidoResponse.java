package mx.ideacom.ids.common.dto;

/** Respuesta al latido: el servidor puede ajustar el intervalo del agente. */
public record LatidoResponse(
        Integer intervaloSegundos,
        String estadoRegistro) {
}
