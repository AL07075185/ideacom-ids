package mx.ideacom.ids.api.dto;

import java.util.List;

/**
 * Respaldo completo en JSON, portable entre instalaciones. Las relaciones se
 * guardan por clave natural (codigo de sitio, correo, nombre/MAC) y no por id.
 * No incluye tokens de agentes ni sesiones: los agentes se vuelven a registrar
 * y el sistema los reconoce por su MAC.
 */
public record RespaldoDTO(
        Integer version,
        String generado,
        String sistema,
        List<Config> configuracion,
        List<Sitio> sitios,
        List<String> departamentos,
        List<Usuario> usuarios,
        List<FilaDispositivoDTO> dispositivos,
        List<Incidencia> incidencias,
        List<Evento> eventos) {

    public record Config(String clave, String valor) {
    }

    public record Sitio(String nombre, String direccion, String codigoAlta, Boolean activo) {
    }

    public record Usuario(String nombre, String email, String rol, Boolean activo, String passwordHash,
                          String fechaAlta) {
    }

    /** Referencia a un dispositivo: codigo de alta del sitio + MAC (o nombre si no tiene MAC). */
    public record RefDispositivo(String sitio, String mac, String nombre) {
    }

    public record Incidencia(String titulo, String descripcion, String prioridad, String estado,
                             RefDispositivo dispositivo, String creadaPor, String asignadaA, String fechaCreacion,
                             String fechaActualizacion, String fechaCierre) {
    }

    public record Evento(RefDispositivo dispositivo, String tipo, String estadoResultante, String origen,
                         String fechaHora, String fechaFin, String detalle) {
    }
}
