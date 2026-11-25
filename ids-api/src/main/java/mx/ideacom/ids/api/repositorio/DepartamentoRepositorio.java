package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Departamento;

@ApplicationScoped
public class DepartamentoRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public Optional<Departamento> buscarPorId(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(em.find(Departamento.class, id));
    }

    public Optional<Departamento> buscarPorNombre(String nombre) {
        return em.createQuery("SELECT d FROM Departamento d WHERE LOWER(d.nombre) = LOWER(:n)", Departamento.class)
                .setParameter("n", nombre).getResultStream().findFirst();
    }

    public boolean existeNombre(String nombre) {
        return em.createQuery("SELECT COUNT(d) FROM Departamento d WHERE LOWER(d.nombre) = LOWER(:n)", Long.class)
                .setParameter("n", nombre).getSingleResult() > 0;
    }

    public List<Departamento> listar() {
        return em.createQuery("SELECT d FROM Departamento d ORDER BY d.nombre", Departamento.class)
                .getResultList();
    }

    public Departamento guardar(Departamento d) {
        em.persist(d);
        em.flush();
        return d;
    }

    public void eliminar(Departamento d) {
        em.remove(em.contains(d) ? d : em.merge(d));
    }
}
