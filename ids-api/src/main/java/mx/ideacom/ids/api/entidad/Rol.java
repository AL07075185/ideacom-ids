package mx.ideacom.ids.api.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rol")
public class Rol {

    public static final String ADMIN = "ADMIN";
    public static final String USUARIO = "USUARIO";

    @Id
    @Column(name = "id_rol")
    private Short id;

    @Column(name = "nombre", nullable = false, length = 30, unique = true)
    private String nombre;

    @Column(name = "descripcion", length = 150)
    private String descripcion;

    public Short getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean esAdmin() {
        return ADMIN.equals(nombre);
    }
}
