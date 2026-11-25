package mx.ideacom.ids.api.excepcion;

/** Error de negocio con su codigo HTTP. Se convierte a JSON en ApiExceptionMapper. */
public class ApiException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int status;

    public ApiException(int status, String mensaje) {
        super(mensaje);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static ApiException peticionInvalida(String mensaje) {
        return new ApiException(400, mensaje);
    }

    public static ApiException noAutenticado(String mensaje) {
        return new ApiException(401, mensaje);
    }

    public static ApiException prohibido(String mensaje) {
        return new ApiException(403, mensaje);
    }

    public static ApiException noEncontrado(String mensaje) {
        return new ApiException(404, mensaje);
    }

    public static ApiException conflicto(String mensaje) {
        return new ApiException(409, mensaje);
    }
}
