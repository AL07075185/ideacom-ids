package mx.ideacom.ids.api.repositorio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Configuracion;

@ApplicationScoped
public class ConfiguracionRepositorio {

    @PersistenceContext(unitName = "idsPU")
    private EntityManager em;

    public java.util.List<Configuracion> listar() {
        return em.createQuery("SELECT c FROM Configuracion c ORDER BY c.clave", Configuracion.class).getResultList();
    }

    public Optional<Configuracion> buscar(String clave) {
        return Optional.ofNullable(em.find(Configuracion.class, clave));
    }
}
