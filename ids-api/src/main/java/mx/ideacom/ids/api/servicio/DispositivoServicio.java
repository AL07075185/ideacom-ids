package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;
import java.util.Set;
import mx.ideacom.ids.api.dto.DisponibilidadDTO;
import mx.ideacom.ids.api.dto.DispositivoDTO;
import mx.ideacom.ids.api.dto.DispositivoGuardarRequest;
import mx.ideacom.ids.api.dto.EventoDTO;
import mx.ideacom.ids.api.dto.MapaDTO;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.OrigenEvento;
import mx.ideacom.ids.api.entidad.TipoDispositivo;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DepartamentoRepositorio;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.reglas.DisponibilidadCalculadora;
import mx.ideacom.ids.common.util.Fechas;

/** Consulta y administracion de dispositivos desde el panel. */
@ApplicationScoped
@Transactional
public class DispositivoServicio {

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private DepartamentoRepositorio departamentos;

    @Inject
    private UsuarioRepositorio usuarios;

    // ------------------------------------------------------------ consultas

    public List<DispositivoDTO> listar(Sujeto s, String registro) {
        EstadoRegistro filtro = Textos.enumeracion(EstadoRegistro.class, registro, "registro");
        Integer idResponsable = s.admin() ? null : s.id();
        return dispositivos.listar(filtro, idResponsable).stream().map(DispositivoDTO::de).toList();
    }

    public DispositivoDTO obtener(Sujeto s, Integer id) {
        return DispositivoDTO.de(conAcceso(s, id));
    }

    public List<EventoDTO> eventos(Sujeto s, Integer id, int limite) {
        conAcceso(s, id);
        int n = Math.max(1, Math.min(limite, 500));
        return eventos.ultimosDeDispositivo(id, n).stream().map(EventoDTO::de).toList();
    }

    public DisponibilidadDTO disponibilidad(Sujeto s, Integer id, int horas) {
        Dispositivo d = conAcceso(s, id);
        int h = Math.max(1, Math.min(horas, 24 * 30));
        OptionalDouble p = calcularDisponibilidad(d.getId(), h);
        return new DisponibilidadDTO(d.getId(), h, p.isPresent() ? p.getAsDouble() : null);
    }

    /** Tiempos disponible, caido y apagado de un dispositivo en las ultimas {@code horas}. */
    public DisponibilidadCalculadora.Medicion medir(Integer idDispositivo, int horas) {
        LocalDateTime fin = Fechas.ahoraUtc();
        LocalDateTime inicio = fin.minusHours(horas);
        EstadoDispositivo inicial = eventos.estadoVigenteEn(idDispositivo, inicio)
                .orElse(EstadoDispositivo.DESCONOCIDO);
        List<DisponibilidadCalculadora.Cambio> cambios = eventos.cambiosEnVentana(idDispositivo, inicio, fin)
                .stream()
                .map(e -> new DisponibilidadCalculadora.Cambio(e.getFechaHora(), e.getEstadoResultante()))
                .toList();
        return DisponibilidadCalculadora.medir(inicial, cambios, inicio, fin);
    }

    /** Disponibilidad de un dispositivo en las ultimas {@code horas}. */
    public OptionalDouble calcularDisponibilidad(Integer idDispositivo, int horas) {
        return medir(idDispositivo, horas).porcentaje();
    }

    public MapaDTO mapa(Sujeto s) {
        List<Dispositivo> lista = dispositivos.listar(EstadoRegistro.APROBADO, s.admin() ? null : s.id());
        Set<Integer> ids = new HashSet<>();
        lista.forEach(d -> ids.add(d.getId()));
        List<MapaDTO.Nodo> nodos = lista.stream()
                .map(d -> new MapaDTO.Nodo(d.getId(), d.getNombre(), d.getTipo().name(), d.getIp(),
                        d.getEstadoActual().name(), d.getSitio().getNombre()))
                .toList();
        List<MapaDTO.Arista> aristas = lista.stream()
                .filter(d -> d.getConectadoA() != null && ids.contains(d.getConectadoA().getId()))
                .map(d -> new MapaDTO.Arista(d.getConectadoA().getId(), d.getId()))
                .toList();
        return new MapaDTO(nodos, aristas);
    }

    // ------------------------------------------------------------ administracion

    /** Alta manual de un equipo sin agente (switch, router, camara, impresora). */
    public DispositivoDTO crearManual(DispositivoGuardarRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        LocalDateTime ahora = Fechas.ahoraUtc();
        Dispositivo d = new Dispositivo();
        d.setSitio(sitios.buscarPorId(req.idSitio())
                .orElseThrow(() -> ApiException.peticionInvalida("Sitio inválido")));
        d.setConAgente(false);
        d.setEstadoRegistro(EstadoRegistro.APROBADO);
        d.setEstadoActual(EstadoDispositivo.SIN_AGENTE);
        d.setFechaAlta(ahora);
        d.setFechaAprobacion(ahora);
        aplicarCamposAdministrativos(d, req);
        aplicarRedManual(d, req);
        d = dispositivos.guardar(d);
        registrarEvento(d, TipoEvento.REGISTRO, EstadoDispositivo.SIN_AGENTE, "Alta manual desde el panel");
        return DispositivoDTO.de(d);
    }

    public DispositivoDTO actualizar(Integer id, DispositivoGuardarRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        Dispositivo d = existente(id);
        aplicarCamposAdministrativos(d, req);
        if (!d.isConAgente()) {
            aplicarRedManual(d, req);
        }
        return DispositivoDTO.de(dispositivos.guardar(d));
    }

    public DispositivoDTO aprobar(Integer id) {
        Dispositivo d = existente(id);
        if (d.getEstadoRegistro() != EstadoRegistro.APROBADO) {
            d.setEstadoRegistro(EstadoRegistro.APROBADO);
            d.setFechaAprobacion(Fechas.ahoraUtc());
            registrarEvento(d, TipoEvento.APROBADO, null, "Alta aprobada por el administrador");
        }
        return DispositivoDTO.de(dispositivos.guardar(d));
    }

    public DispositivoDTO rechazar(Integer id) {
        Dispositivo d = existente(id);
        if (!d.isConAgente()) {
            throw ApiException.peticionInvalida("Un equipo sin agente no se rechaza; elimínalo");
        }
        d.setEstadoRegistro(EstadoRegistro.RECHAZADO);
        d.setTokenHash(null);
        d.setEstadoActual(EstadoDispositivo.DESCONOCIDO);
        registrarEvento(d, TipoEvento.RECHAZADO, EstadoDispositivo.DESCONOCIDO, "Alta rechazada; token revocado");
        return DispositivoDTO.de(dispositivos.guardar(d));
    }

    public void eliminar(Integer id) {
        Dispositivo d = existente(id);
        dispositivos.desconectarHijos(d.getId());
        dispositivos.eliminar(d);
    }

    // ------------------------------------------------------------ auxiliares

    private Dispositivo existente(Integer id) {
        return dispositivos.buscarPorId(id)
                .orElseThrow(() -> ApiException.noEncontrado("Dispositivo no encontrado"));
    }

    /** El administrador ve todo; un usuario solo los equipos de los que es responsable. */
    private Dispositivo conAcceso(Sujeto s, Integer id) {
        Dispositivo d = existente(id);
        if (!s.admin() && (d.getResponsable() == null || !d.getResponsable().getId().equals(s.id()))) {
            throw ApiException.prohibido("No tienes acceso a este dispositivo");
        }
        return d;
    }

    private void aplicarCamposAdministrativos(Dispositivo d, DispositivoGuardarRequest r) {
        d.setNombre(Textos.obligatorio(r.nombre(), 100, "nombre"));
        TipoDispositivo tipo = Textos.enumeracion(TipoDispositivo.class, r.tipo(), "tipo");
        if (tipo != null) {
            d.setTipo(tipo);
        }
        d.setUbicacion(Textos.limpio(r.ubicacion(), 120));
        d.setNotas(Textos.limpio(r.notas(), 500));
        d.setDepartamento(r.idDepartamento() == null ? null : departamentos.buscarPorId(r.idDepartamento())
                .orElseThrow(() -> ApiException.peticionInvalida("Departamento inválido")));
        d.setResponsable(r.idResponsable() == null ? null : usuarios.buscarPorId(r.idResponsable())
                .orElseThrow(() -> ApiException.peticionInvalida("Responsable inválido")));
        d.setConectadoA(validarConexion(d, r.idConectadoA()));
    }

    private void aplicarRedManual(Dispositivo d, DispositivoGuardarRequest r) {
        d.setIp(Textos.limpio(r.ip(), 45));
        d.setIpv6(Textos.limpio(r.ipv6(), 45));
        String mac = Textos.mac(r.mac());
        if (mac != null) {
            dispositivos.buscarPorSitioYMac(d.getSitio().getId(), mac)
                    .filter(otro -> !otro.getId().equals(d.getId()))
                    .ifPresent(otro -> {
                        throw ApiException.conflicto("Ya existe un equipo con esa MAC en el sitio");
                    });
        }
        d.setMac(mac);
    }

    /** Evita que un equipo quede conectado a sí mismo o forme un ciclo. */
    private Dispositivo validarConexion(Dispositivo d, Integer idPadre) {
        if (idPadre == null) {
            return null;
        }
        if (idPadre.equals(d.getId())) {
            throw ApiException.peticionInvalida("Un equipo no puede conectarse a sí mismo");
        }
        Dispositivo padre = existente(idPadre);
        Dispositivo actual = padre;
        int saltos = 0;
        while (actual != null && saltos++ < 100) {
            if (d.getId() != null && d.getId().equals(actual.getId())) {
                throw ApiException.peticionInvalida("La conexión formaría un ciclo en el mapa");
            }
            actual = actual.getConectadoA();
        }
        return padre;
    }

    private void registrarEvento(Dispositivo d, TipoEvento tipo, EstadoDispositivo estado, String detalle) {
        LocalDateTime ahora = Fechas.ahoraUtc();
        Evento e = new Evento(d, tipo, estado, OrigenEvento.PANEL, ahora, detalle);
        e.setFechaRegistro(ahora);
        eventos.guardar(e);
    }
}
