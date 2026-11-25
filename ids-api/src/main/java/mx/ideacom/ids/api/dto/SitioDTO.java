package mx.ideacom.ids.api.dto;

public record SitioDTO(Integer id, String nombre, String direccion, String codigoAlta, boolean activo,
                       long dispositivos) {
}
