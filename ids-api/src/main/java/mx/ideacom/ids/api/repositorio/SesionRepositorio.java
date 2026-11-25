package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.time.LocalDateTime;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Sesion;

@ApplicationScoped
public class SesionRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public void guardar(Sesion s) {
        em.persist(s);
    }

    public Optional<Sesion> buscarVigente(String tokenHash, LocalDateTime ahora) {
        return em.createQuery(
                        "SELECT s FROM Sesion s WHERE s.tokenHash = :h AND s.fechaExpira > :ahora", Sesion.class)
                .setParameter("h", tokenHash)
                .setParameter("ahora", ahora)
                .getResultStream().findFirst();
    }

    public int eliminarPorHash(String tokenHash) {
        return em.createQuery("DELETE FROM Sesion s WHERE s.tokenHash = :h")
                .setParameter("h", tokenHash).executeUpdate();
    }

    public int eliminarExpiradas(LocalDateTime ahora) {
        return em.createQuery("DELETE FROM Sesion s WHERE s.fechaExpira <= :ahora")
                .setParameter("ahora", ahora).executeUpdate();
    }
}
