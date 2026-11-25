package mx.ideacom.ids.api.dto;

public record LoginResponse(String token, Integer id, String nombre, String email, String rol, String expira) {
}
