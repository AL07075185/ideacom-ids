package mx.ideacom.ids.common.dto;

/** Respuesta al registro: el token solo se entrega aqui, una vez. */
public record RegistroAgenteResponse(
        Integer dispositivoId,
        String token,
        Integer intervaloSegundos,
        String estadoRegistro) {
}
