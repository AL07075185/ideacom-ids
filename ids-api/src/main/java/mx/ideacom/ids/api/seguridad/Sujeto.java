package mx.ideacom.ids.api.seguridad;

/**
 * Quien hace la peticion: un usuario del panel o un agente.
 *
 * @param tipo   USUARIO o AGENTE
 * @param id     id_usuario o id_dispositivo
 * @param nombre nombre para mostrar
 * @param admin  true si es usuario con rol ADMIN
 */
public record Sujeto(Tipo tipo, Integer id, String nombre, boolean admin) implements java.io.Serializable {

    public enum Tipo { USUARIO, AGENTE }

    public boolean esUsuario() {
        return tipo == Tipo.USUARIO;
    }

    public boolean esAgente() {
        return tipo == Tipo.AGENTE;
    }
}
