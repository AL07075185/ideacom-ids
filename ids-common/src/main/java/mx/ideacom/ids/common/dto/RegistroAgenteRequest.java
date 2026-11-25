package mx.ideacom.ids.common.dto;

/** Datos que envía el agente al darse de alta. */
public record RegistroAgenteRequest(
        String codigoAlta,
        String hostname,
        String ip,
        String ipv6,
        String mac,
        String soNombre,
        String soVersion,
        String arquitectura,
        String cpuModelo,
        Long ramMb,
        Long discoGb,
        String versionAgente) {
}
