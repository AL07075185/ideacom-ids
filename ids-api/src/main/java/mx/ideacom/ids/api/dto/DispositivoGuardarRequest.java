package mx.ideacom.ids.api.dto;

/**
 * Campos administrativos de un dispositivo. En el alta manual (equipo sin
 * agente) tambien se usan idSitio, ip y mac.
 */
public record DispositivoGuardarRequest(
        String nombre,
        String tipo,
        String ubicacion,
        String notas,
        Integer idSitio,
        Integer idDepartamento,
        Integer idResponsable,
        Integer idConectadoA,
        String ip,
        String ipv6,
        String mac) {
}
