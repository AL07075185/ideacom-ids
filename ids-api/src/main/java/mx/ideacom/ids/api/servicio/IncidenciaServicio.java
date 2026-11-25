package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import mx.ideacom.ids.api.dto.IncidenciaActualizarRequest;
import mx.ideacom.ids.api.dto.IncidenciaCrearRequest;
import mx.ideacom.ids.api.dto.IncidenciaDTO;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.EstadoIncidencia;
import mx.ideacom.ids.api.entidad.Incidencia;
import mx.ideacom.ids.api.entidad.PrioridadIncidencia;
import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.IncidenciaRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.common.util.Fechas;

@ApplicationScoped
@Transactional
public class IncidenciaServicio {

    @Inject
    private IncidenciaRepositorio incidencias;

    @Inject
    private UsuarioRepositorio usuarios;

    @Inject
    private DispositivoRepositorio dispositivos;

    /** El administrador ve todas; un usuario, las que creo o tiene asignadas. */
    public List<IncidenciaDTO> listar(Sujeto s, String estado) {
        EstadoIncidencia filtro = Textos.enumeracion(EstadoIncidencia.class, estado, "estado");
        return incidencias.listar(filtro, s.admin() ? null : s.id()).stream().map(IncidenciaDTO::de).toList();
    }

    public IncidenciaDTO crear(Sujeto s, IncidenciaCrearRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        Usuario creador = usuarios.buscarPorId(s.id())
                .orElseThrow(() -> ApiException.noAutenticado("Usuario no encontrado"));
        Incidencia i = new Incidencia();
        i.setTitulo(Textos.obligatorio(req.titulo(), 150, "título"));
        i.setDescripcion(Textos.limpio(req.descripcion(), 5000));
        PrioridadIncidencia p = Textos.enumeracion(PrioridadIncidencia.class, req.prioridad(), "prioridad");
        i.setPrioridad(p == null ? PrioridadIncidencia.MEDIA : p);
        i.setEstado(EstadoIncidencia.ABIERTA);
        if (req.idDispositivo() != null) {
            Dispositivo d = dispositivos.buscarPorId(req.idDispositivo())
                    .orElseThrow(() -> ApiException.peticionInvalida("Dispositivo inválido"));
            boolean propio = d.getResponsable() != null && d.getResponsable().getId().equals(s.id());
            if (!s.admin() && !propio) {
                throw ApiException.prohibido("Solo puedes reportar equipos que tienes asignados");
            }
            i.setDispositivo(d);
        }
        LocalDateTime ahora = Fechas.ahoraUtc();
        i.setCreadaPor(creador);
        i.setFechaCreacion(ahora);
        i.setFechaActualizacion(ahora);
        return IncidenciaDTO.de(incidencias.guardar(i));
    }

    /** Solo administrador: estado, prioridad y asignacion. */
    public IncidenciaDTO actualizar(Integer id, IncidenciaActualizarRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        Incidencia i = incidencias.buscarPorId(id)
                .orElseThrow(() -> ApiException.noEncontrado("Incidencia no encontrada"));
        LocalDateTime ahora = Fechas.ahoraUtc();

        EstadoIncidencia estado = Textos.enumeracion(EstadoIncidencia.class, req.estado(), "estado");
        if (estado != null && estado != i.getEstado()) {
            i.setEstado(estado);
            i.setFechaCierre(estado == EstadoIncidencia.CERRADA ? ahora : null);
        }
        PrioridadIncidencia prioridad = Textos.enumeracion(PrioridadIncidencia.class, req.prioridad(), "prioridad");
        if (prioridad != null) {
            i.setPrioridad(prioridad);
        }
        if (req.idAsignadaA() != null) {
            if (req.idAsignadaA() == 0) {
                i.setAsignadaA(null);
            } else {
                Usuario u = usuarios.buscarPorId(req.idAsignadaA())
                        .filter(Usuario::isActivo)
                        .orElseThrow(() -> ApiException.peticionInvalida("Usuario asignado inválido"));
                i.setAsignadaA(u);
            }
        }
        i.setFechaActualizacion(ahora);
        return IncidenciaDTO.de(incidencias.guardar(i));
    }
}
