package mx.ideacom.ids.api.dto;

import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.common.util.Fechas;

public record UsuarioDTO(Integer id, String nombre, String email, String rol, boolean activo, String fechaAlta) {

    public static UsuarioDTO de(Usuario u) {
        return new UsuarioDTO(u.getId(), u.getNombre(), u.getEmail(),
                u.getRol() == null ? null : u.getRol().getNombre(), u.isActivo(), Fechas.iso(u.getFechaAlta()));
    }
}
