package mx.ideacom.ids.common.dto;

/** Latido periódico del agente. */
public record LatidoRequest(
        String ip,
        String ipv6,
        Boolean internetOk,
        String versionAgente) {
}
