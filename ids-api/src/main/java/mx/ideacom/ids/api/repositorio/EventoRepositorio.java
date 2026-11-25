package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;

@ApplicationScoped
public class EventoRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public void guardar(Evento e) {
        em.persist(e);
    }

    public List<Evento> ultimosDeDispositivo(Integer idDispositivo, int limite) {
        return em.createQuery("SELECT e FROM Evento e WHERE e.dispositivo.id = :id"
                        + " ORDER BY e.fechaHora DESC, e.id DESC", Evento.class)
                .setParameter("id", idDispositivo)
                .setMaxResults(limite)
                .getResultList();
    }

    /** Ultimos eventos de todos los dispositivos (o solo de los de un responsable). */
    public List<Evento> ultimos(int limite, Integer idResponsable) {
        String jpql = "SELECT e FROM Evento e"
                + (idResponsable != null ? " WHERE e.dispositivo.responsable.id = :resp" : "")
                + " ORDER BY e.fechaHora DESC, e.id DESC";
        TypedQuery<Evento> q = em.createQuery(jpql, Evento.class).setMaxResults(limite);
        if (idResponsable != null) {
            q.setParameter("resp", idResponsable);
        }
        return q.getResultList();
    }

    /** Cambios de estado dentro de la ventana, ordenados por fecha. */
    public List<Evento> cambiosEnVentana(Integer idDispositivo, LocalDateTime inicio, LocalDateTime fin) {
        return em.createQuery("SELECT e FROM Evento e WHERE e.dispositivo.id = :id"
                        + " AND e.estadoResultante IS NOT NULL AND e.fechaHora > :ini AND e.fechaHora <= :fin"
                        + " ORDER BY e.fechaHora, e.id", Evento.class)
                .setParameter("id", idDispositivo)
                .setParameter("ini", inicio)
                .setParameter("fin", fin)
                .getResultList();
    }

    /** Eventos de un dispositivo desde una fecha (para exportar su historial). */
    public List<Evento> deDispositivoDesde(Integer idDispositivo, LocalDateTime desde) {
        return em.createQuery("SELECT e FROM Evento e WHERE e.dispositivo.id = :id AND e.fechaHora >= :d"
                        + " ORDER BY e.fechaHora, e.id", Evento.class)
                .setParameter("id", idDispositivo).setParameter("d", desde).getResultList();
    }

    public long contarDesde(Integer idDispositivo, mx.ideacom.ids.common.modelo.TipoEvento tipo, LocalDateTime desde) {
        return em.createQuery("SELECT COUNT(e) FROM Evento e WHERE e.dispositivo.id = :id AND e.tipo = :t"
                        + " AND e.fechaHora >= :d", Long.class)
                .setParameter("id", idDispositivo).setParameter("t", tipo).setParameter("d", desde)
                .getSingleResult();
    }

    /** Eventos de falla o apagado desde una fecha, mas recientes primero (para el reporte). */
    public List<Evento> relevantesDesde(LocalDateTime desde, int limite) {
        return em.createQuery("SELECT e FROM Evento e WHERE e.fechaHora >= :d AND e.tipo IN :tipos"
                        + " ORDER BY e.fechaHora DESC, e.id DESC", Evento.class)
                .setParameter("d", desde)
                .setParameter("tipos", List.of(mx.ideacom.ids.common.modelo.TipoEvento.DESCONEXION_INESPERADA,
                        mx.ideacom.ids.common.modelo.TipoEvento.APAGADO,
                        mx.ideacom.ids.common.modelo.TipoEvento.SIN_INTERNET,
                        mx.ideacom.ids.common.modelo.TipoEvento.SIN_CONEXION_SERVIDOR))
                .setMaxResults(limite)
                .getResultList();
    }

    /** Todos los eventos (para el respaldo completo). */
    public List<Evento> todos() {
        return em.createQuery("SELECT e FROM Evento e ORDER BY e.fechaHora, e.id", Evento.class).getResultList();
    }

    public boolean existe(Integer idDispositivo, mx.ideacom.ids.common.modelo.TipoEvento tipo, LocalDateTime fecha) {
        return em.createQuery("SELECT COUNT(e) FROM Evento e WHERE e.dispositivo.id = :id AND e.tipo = :t"
                        + " AND e.fechaHora = :f", Long.class)
                .setParameter("id", idDispositivo).setParameter("t", tipo).setParameter("f", fecha)
                .getSingleResult() > 0;
    }

    /** Ultima desconexion inesperada registrada hasta una fecha. */
    public Optional<Evento> ultimaDesconexionHasta(Integer idDispositivo, LocalDateTime fecha) {
        return em.createQuery("SELECT e FROM Evento e WHERE e.dispositivo.id = :id AND e.tipo = :tipo"
                        + " AND e.fechaHora <= :f ORDER BY e.fechaHora DESC, e.id DESC", Evento.class)
                .setParameter("id", idDispositivo)
                .setParameter("tipo", mx.ideacom.ids.common.modelo.TipoEvento.DESCONEXION_INESPERADA)
                .setParameter("f", fecha)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }

    /** Ultimo estado registrado antes (o en) una fecha. */
    public Optional<EstadoDispositivo> estadoVigenteEn(Integer idDispositivo, LocalDateTime fecha) {
        return em.createQuery("SELECT e.estadoResultante FROM Evento e WHERE e.dispositivo.id = :id"
                        + " AND e.estadoResultante IS NOT NULL AND e.fechaHora <= :f"
                        + " ORDER BY e.fechaHora DESC, e.id DESC", EstadoDispositivo.class)
                .setParameter("id", idDispositivo)
                .setParameter("f", fecha)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }
}
