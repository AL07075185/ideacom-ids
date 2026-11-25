package mx.ideacom.ids.api.dto;

import java.util.List;
import java.util.Map;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.common.util.Fechas;

/**
 * Formato portable de un dispositivo para exportar e importar (CSV o JSON).
 * Usa nombres en lugar de ids (sitio, departamento, correo del responsable,
 * nombre del equipo al que se conecta) para poder llevarlo a otra instalacion.
 * Al importar solo se usan los campos administrativos y de red; el resto es informativo.
 */
public record FilaDispositivoDTO(
        String nombre,
        String tipo,
        String sitio,
        String departamento,
        String responsable,
        String conectadoA,
        String ubicacion,
        String ip,
        String ipv6,
        String mac,
        Boolean conAgente,
        String hostname,
        String soNombre,
        String soVersion,
        String cpuModelo,
        Long ramMb,
        Long discoGb,
        String estadoRegistro,
        String estadoActual,
        String ultimoLatido,
        String notas) {

    /** Encabezados del CSV, en el mismo orden que los campos. */
    public static final List<String> COLUMNAS = List.of("nombre", "tipo", "sitio", "departamento", "responsable",
            "conectadoA", "ubicacion", "ip", "ipv6", "mac", "conAgente", "hostname", "soNombre", "soVersion",
            "cpuModelo", "ramMb", "discoGb", "estadoRegistro", "estadoActual", "ultimoLatido", "notas");

    /** @param sitioPorCodigo true para usar el codigo de alta del sitio (respaldos) en vez del nombre */
    public static FilaDispositivoDTO de(Dispositivo d, boolean sitioPorCodigo) {
        return new FilaDispositivoDTO(d.getNombre(), d.getTipo().name(),
                sitioPorCodigo ? d.getSitio().getCodigoAlta() : d.getSitio().getNombre(),
                d.getDepartamento() == null ? null : d.getDepartamento().getNombre(),
                d.getResponsable() == null ? null : d.getResponsable().getEmail(),
                d.getConectadoA() == null ? null : d.getConectadoA().getNombre(),
                d.getUbicacion(), d.getIp(), d.getIpv6(), d.getMac(), d.isConAgente(), d.getHostname(),
                d.getSoNombre(), d.getSoVersion(), d.getCpuModelo(), d.getRamMb(), d.getDiscoGb(),
                d.getEstadoRegistro().name(), d.getEstadoActual().name(), Fechas.iso(d.getUltimoLatido()),
                d.getNotas());
    }

    public List<Object> valores() {
        return java.util.Arrays.asList(nombre, tipo, sitio, departamento, responsable, conectadoA, ubicacion, ip,
                ipv6, mac, conAgente, hostname, soNombre, soVersion, cpuModelo, ramMb, discoGb, estadoRegistro,
                estadoActual, ultimoLatido, notas);
    }

    /** Construye la fila a partir de un renglon de CSV (claves ya normalizadas). */
    public static FilaDispositivoDTO desdeCsv(Map<String, String> f) {
        return new FilaDispositivoDTO(t(f, "nombre"), t(f, "tipo"), t(f, "sitio"), t(f, "departamento"),
                t(f, "responsable"), t(f, "conectadoa"), t(f, "ubicacion"), t(f, "ip"), t(f, "ipv6"), t(f, "mac"),
                booleano(t(f, "conagente")), t(f, "hostname"), t(f, "sonombre"), t(f, "soversion"),
                t(f, "cpumodelo"), entero(t(f, "rammb")), entero(t(f, "discogb")), t(f, "estadoregistro"),
                t(f, "estadoactual"), t(f, "ultimolatido"), t(f, "notas"));
    }

    private static String t(Map<String, String> f, String clave) {
        String v = f.get(clave);
        return v == null || v.isBlank() ? null : v.trim();
    }

    private static Boolean booleano(String v) {
        if (v == null) {
            return null;
        }
        String x = v.toLowerCase(java.util.Locale.ROOT);
        return x.equals("true") || x.equals("si") || x.equals("sí") || x.equals("1") || x.equals("verdadero");
    }

    private static Long entero(String v) {
        try {
            return v == null ? null : Long.valueOf(v);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
