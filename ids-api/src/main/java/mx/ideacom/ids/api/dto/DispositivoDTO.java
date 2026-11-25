package mx.ideacom.ids.api.dto;

import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.common.util.Fechas;

/** Vista del dispositivo para el panel (sin el hash del token). */
public record DispositivoDTO(
        Integer id,
        String nombre,
        String tipo,
        String ubicacion,
        String notas,
        Integer idSitio,
        String sitio,
        Integer idDepartamento,
        String departamento,
        Integer idResponsable,
        String responsable,
        Integer idConectadoA,
        String conectadoA,
        boolean conAgente,
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
        String versionAgente,
        String estadoRegistro,
        String estadoActual,
        Boolean internetOk,
        String ultimoLatido,
        String fechaAlta) {

    public static DispositivoDTO de(Dispositivo d) {
        return new DispositivoDTO(
                d.getId(), d.getNombre(), d.getTipo().name(), d.getUbicacion(), d.getNotas(),
                d.getSitio() == null ? null : d.getSitio().getId(),
                d.getSitio() == null ? null : d.getSitio().getNombre(),
                d.getDepartamento() == null ? null : d.getDepartamento().getId(),
                d.getDepartamento() == null ? null : d.getDepartamento().getNombre(),
                d.getResponsable() == null ? null : d.getResponsable().getId(),
                d.getResponsable() == null ? null : d.getResponsable().getNombre(),
                d.getConectadoA() == null ? null : d.getConectadoA().getId(),
                d.getConectadoA() == null ? null : d.getConectadoA().getNombre(),
                d.isConAgente(), d.getHostname(), d.getIp(), d.getIpv6(), d.getMac(), d.getSoNombre(), d.getSoVersion(),
                d.getArquitectura(), d.getCpuModelo(), d.getRamMb(), d.getDiscoGb(), d.getVersionAgente(),
                d.getEstadoRegistro().name(), d.getEstadoActual().name(), d.getInternetOk(),
                Fechas.iso(d.getUltimoLatido()), Fechas.iso(d.getFechaAlta()));
    }
}
