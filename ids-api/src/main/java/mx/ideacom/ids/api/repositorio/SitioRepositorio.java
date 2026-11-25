package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Sitio;

@ApplicationScoped
public class SitioRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public Optional<Sitio> buscarPorId(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(em.find(Sitio.class, id));
    }

    public Optional<Sitio> buscarActivoPorCodigo(String codigo) {
        return em.createQuery("SELECT s FROM Sitio s WHERE s.codigoAlta = :c AND s.activo = true", Sitio.class)
                .setParameter("c", codigo)
                .getResultStream().findFirst();
    }

    /** Busca por codigo de alta exacto o, si no, por nombre (sin distinguir mayusculas). */
    public Optional<Sitio> buscarPorCodigoONombre(String texto) {
        Optional<Sitio> porCodigo = em.createQuery("SELECT s FROM Sitio s WHERE s.codigoAlta = :c", Sitio.class)
                .setParameter("c", texto).getResultStream().findFirst();
        if (porCodigo.isPresent()) {
            return porCodigo;
        }
        return em.createQuery("SELECT s FROM Sitio s WHERE LOWER(s.nombre) = LOWER(:n) ORDER BY s.id", Sitio.class)
                .setParameter("n", texto).setMaxResults(1).getResultStream().findFirst();
    }

    public boolean existeCodigo(String codigo) {
        return em.createQuery("SELECT COUNT(s) FROM Sitio s WHERE s.codigoAlta = :c", Long.class)
                .setParameter("c", codigo).getSingleResult() > 0;
    }

    public List<Sitio> listar() {
        return em.createQuery("SELECT s FROM Sitio s ORDER BY s.nombre", Sitio.class).getResultList();
    }

    public long contarDispositivos(Integer idSitio) {
        return em.createQuery("SELECT COUNT(d) FROM Dispositivo d WHERE d.sitio.id = :id", Long.class)
                .setParameter("id", idSitio).getSingleResult();
    }

    public Sitio guardar(Sitio s) {
        if (s.getId() == null) {
            em.persist(s);
            em.flush();
            return s;
        }
        return em.merge(s);
    }
}
