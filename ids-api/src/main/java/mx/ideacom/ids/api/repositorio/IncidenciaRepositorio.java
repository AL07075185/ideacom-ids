package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.EstadoIncidencia;
import mx.ideacom.ids.api.entidad.Incidencia;

@ApplicationScoped
public class IncidenciaRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public Optional<Incidencia> buscarPorId(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(em.find(Incidencia.class, id));
    }

    /** Lista incidencias; si idUsuario no es null, solo las que creo o tiene asignadas. */
    public List<Incidencia> listar(EstadoIncidencia estado, Integer idUsuario) {
        List<String> condiciones = new ArrayList<>();
        if (estado != null) {
            condiciones.add("i.estado = :estado");
        }
        if (idUsuario != null) {
            condiciones.add("(i.creadaPor.id = :u OR a.id = :u)");
        }
        String jpql = "SELECT i FROM Incidencia i LEFT JOIN i.asignadaA a"
                + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones))
                + " ORDER BY i.fechaCreacion DESC";
        TypedQuery<Incidencia> q = em.createQuery(jpql, Incidencia.class);
        if (estado != null) {
            q.setParameter("estado", estado);
        }
        if (idUsuario != null) {
            q.setParameter("u", idUsuario);
        }
        return q.getResultList();
    }

    public boolean existe(String titulo, java.time.LocalDateTime fechaCreacion, Integer idCreadaPor) {
        return em.createQuery("SELECT COUNT(i) FROM Incidencia i WHERE i.titulo = :t AND i.fechaCreacion = :f"
                        + " AND i.creadaPor.id = :u", Long.class)
                .setParameter("t", titulo).setParameter("f", fechaCreacion).setParameter("u", idCreadaPor)
                .getSingleResult() > 0;
    }

    public long contarNoCerradas(Integer idUsuario) {
        String jpql = "SELECT COUNT(i) FROM Incidencia i LEFT JOIN i.asignadaA a WHERE i.estado <> :cerrada"
                + (idUsuario != null ? " AND (i.creadaPor.id = :u OR a.id = :u)" : "");
        TypedQuery<Long> q = em.createQuery(jpql, Long.class).setParameter("cerrada", EstadoIncidencia.CERRADA);
        if (idUsuario != null) {
            q.setParameter("u", idUsuario);
        }
        return q.getSingleResult();
    }

    public Incidencia guardar(Incidencia i) {
        if (i.getId() == null) {
            em.persist(i);
            em.flush();
            return i;
        }
        return em.merge(i);
    }
}
