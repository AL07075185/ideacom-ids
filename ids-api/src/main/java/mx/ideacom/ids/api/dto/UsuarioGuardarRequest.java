package mx.ideacom.ids.api.dto;

/** Alta o edicion de usuario. En edicion, password vacio = no cambiar. */
public record UsuarioGuardarRequest(String nombre, String email, String password, String rol, Boolean activo) {
}
