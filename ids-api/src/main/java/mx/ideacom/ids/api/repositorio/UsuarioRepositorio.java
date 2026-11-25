package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Rol;
import mx.ideacom.ids.api.entidad.Usuario;

@ApplicationScoped
public class UsuarioRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public Optional<Usuario> buscarPorId(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(em.find(Usuario.class, id));
    }

    public Optional<Usuario> buscarPorEmail(String email) {
        return em.createQuery("SELECT u FROM Usuario u WHERE LOWER(u.email) = LOWER(:email)", Usuario.class)
                .setParameter("email", email)
                .getResultStream().findFirst();
    }

    public List<Usuario> listar() {
        return em.createQuery("SELECT u FROM Usuario u ORDER BY u.nombre", Usuario.class).getResultList();
    }

    public Optional<Rol> rolPorNombre(String nombre) {
        return em.createQuery("SELECT r FROM Rol r WHERE r.nombre = :n", Rol.class)
                .setParameter("n", nombre)
                .getResultStream().findFirst();
    }

    public Usuario guardar(Usuario u) {
        if (u.getId() == null) {
            em.persist(u);
            em.flush();
            return u;
        }
        return em.merge(u);
    }
}
