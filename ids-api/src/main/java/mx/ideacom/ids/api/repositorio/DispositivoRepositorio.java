package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.EstadoRegistro;

@ApplicationScoped
public class DispositivoRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public Optional<Dispositivo> buscarPorId(Integer id) {
        return id == null ? Optional.empty() : Optional.ofNullable(em.find(Dispositivo.class, id));
    }

    public Optional<Dispositivo> buscarPorTokenHash(String hash) {
        return em.createQuery("SELECT d FROM Dispositivo d WHERE d.tokenHash = :h", Dispositivo.class)
                .setParameter("h", hash)
                .getResultStream().findFirst();
    }

    public Optional<Dispositivo> buscarPorSitioYMac(Integer idSitio, String mac) {
        return em.createQuery("SELECT d FROM Dispositivo d WHERE d.sitio.id = :s AND d.mac = :m", Dispositivo.class)
                .setParameter("s", idSitio)
                .setParameter("m", mac)
                .getResultStream().findFirst();
    }

    public Optional<Dispositivo> buscarPorSitioYNombre(Integer idSitio, String nombre) {
        return em.createQuery("SELECT d FROM Dispositivo d WHERE d.sitio.id = :s AND LOWER(d.nombre) = LOWER(:n)"
                        + " ORDER BY d.id", Dispositivo.class)
                .setParameter("s", idSitio)
                .setParameter("n", nombre)
                .setMaxResults(1)
                .getResultStream().findFirst();
    }

    /**
     * Lista dispositivos con filtros opcionales.
     *
     * @param registro      estado de registro o null para todos
     * @param idResponsable solo los de este responsable, o null para todos
     */
    public List<Dispositivo> listar(EstadoRegistro registro, Integer idResponsable) {
        List<String> condiciones = new ArrayList<>();
        if (registro != null) {
            condiciones.add("d.estadoRegistro = :reg");
        }
        if (idResponsable != null) {
            condiciones.add("d.responsable.id = :resp");
        }
        String jpql = "SELECT d FROM Dispositivo d"
                + (condiciones.isEmpty() ? "" : " WHERE " + String.join(" AND ", condiciones))
                + " ORDER BY d.nombre";
        TypedQuery<Dispositivo> q = em.createQuery(jpql, Dispositivo.class);
        if (registro != null) {
            q.setParameter("reg", registro);
        }
        if (idResponsable != null) {
            q.setParameter("resp", idResponsable);
        }
        return q.getResultList();
    }

    /** Dispositivos aprobados con agente que deberian estar enviando latidos. */
    public List<Dispositivo> listarVigilables() {
        return em.createQuery("SELECT d FROM Dispositivo d WHERE d.estadoRegistro = :reg AND d.conAgente = true"
                        + " AND d.estadoActual IN :estados", Dispositivo.class)
                .setParameter("reg", EstadoRegistro.APROBADO)
                .setParameter("estados", List.of(EstadoDispositivo.EN_LINEA, EstadoDispositivo.SIN_INTERNET))
                .getResultList();
    }

    /** Conteo de dispositivos aprobados por estado actual. */
    public Map<String, Long> contarPorEstado(Integer idResponsable) {
        String jpql = "SELECT d.estadoActual, COUNT(d) FROM Dispositivo d WHERE d.estadoRegistro = :reg"
                + (idResponsable != null ? " AND d.responsable.id = :resp" : "")
                + " GROUP BY d.estadoActual";
        TypedQuery<Object[]> q = em.createQuery(jpql, Object[].class)
                .setParameter("reg", EstadoRegistro.APROBADO);
        if (idResponsable != null) {
            q.setParameter("resp", idResponsable);
        }
        Map<String, Long> conteo = new LinkedHashMap<>();
        for (EstadoDispositivo e : EstadoDispositivo.values()) {
            conteo.put(e.name(), 0L);
        }
        for (Object[] fila : q.getResultList()) {
            conteo.put(((EstadoDispositivo) fila[0]).name(), (Long) fila[1]);
        }
        return conteo;
    }

    public long contarPendientes() {
        return em.createQuery("SELECT COUNT(d) FROM Dispositivo d WHERE d.estadoRegistro = :reg", Long.class)
                .setParameter("reg", EstadoRegistro.PENDIENTE).getSingleResult();
    }

    /** Quita la referencia "conectado a" de otros equipos antes de borrar uno. */
    public void desconectarHijos(Integer idPadre) {
        em.createQuery("UPDATE Dispositivo d SET d.conectadoA = NULL WHERE d.conectadoA.id = :id")
                .setParameter("id", idPadre).executeUpdate();
    }

    public Dispositivo guardar(Dispositivo d) {
        if (d.getId() == null) {
            em.persist(d);
            em.flush();
            return d;
        }
        return em.merge(d);
    }

    public void eliminar(Dispositivo d) {
        em.remove(em.contains(d) ? d : em.merge(d));
    }
}
