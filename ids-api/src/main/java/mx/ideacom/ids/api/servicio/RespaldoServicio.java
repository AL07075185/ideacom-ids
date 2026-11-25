package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import mx.ideacom.ids.api.dto.FilaDispositivoDTO;
import mx.ideacom.ids.api.dto.RespaldoDTO;
import mx.ideacom.ids.api.dto.ResultadoImportacionDTO;
import mx.ideacom.ids.api.dto.ResultadoRestauracionDTO;
import mx.ideacom.ids.api.dto.ResultadoRestauracionDTO.Seccion;
import mx.ideacom.ids.api.entidad.Configuracion;
import mx.ideacom.ids.api.entidad.Departamento;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.EstadoIncidencia;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.Incidencia;
import mx.ideacom.ids.api.entidad.OrigenEvento;
import mx.ideacom.ids.api.entidad.PrioridadIncidencia;
import mx.ideacom.ids.api.entidad.Rol;
import mx.ideacom.ids.api.entidad.Sitio;
import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.ConfiguracionRepositorio;
import mx.ideacom.ids.api.repositorio.DepartamentoRepositorio;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.IncidenciaRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.util.Fechas;

/**
 * Respaldo y restauracion completos en JSON desde el panel.
 * La restauracion es una fusion: lo que ya existe se actualiza y lo nuevo se
 * crea; nunca borra datos de la instalacion actual.
 */
@ApplicationScoped
@Transactional
public class RespaldoServicio {

    public static final int VERSION = 1;

    @Inject
    private ConfiguracionRepositorio configuracion;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private DepartamentoRepositorio departamentos;

    @Inject
    private UsuarioRepositorio usuarios;

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private IncidenciaRepositorio incidencias;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private ImportacionServicio importacion;

    // ------------------------------------------------------------ generar

    public RespaldoDTO generar(boolean conEventos) {
        List<RespaldoDTO.Config> config = configuracion.listar().stream()
                .map(c -> new RespaldoDTO.Config(c.getClave(), c.getValor())).toList();
        List<RespaldoDTO.Sitio> listaSitios = sitios.listar().stream()
                .map(s -> new RespaldoDTO.Sitio(s.getNombre(), s.getDireccion(), s.getCodigoAlta(), s.isActivo()))
                .toList();
        List<String> deptos = departamentos.listar().stream().map(Departamento::getNombre).toList();
        List<RespaldoDTO.Usuario> listaUsuarios = usuarios.listar().stream()
                .map(u -> new RespaldoDTO.Usuario(u.getNombre(), u.getEmail(), u.getRol().getNombre(), u.isActivo(),
                        u.getPasswordHash(), Fechas.iso(u.getFechaAlta())))
                .toList();
        List<FilaDispositivoDTO> listaDispositivos = dispositivos.listar(null, null).stream()
                .map(d -> FilaDispositivoDTO.de(d, true)).toList();
        List<RespaldoDTO.Incidencia> listaIncidencias = incidencias.listar(null, null).stream()
                .map(i -> new RespaldoDTO.Incidencia(i.getTitulo(), i.getDescripcion(), i.getPrioridad().name(),
                        i.getEstado().name(), ref(i.getDispositivo()), i.getCreadaPor().getEmail(),
                        i.getAsignadaA() == null ? null : i.getAsignadaA().getEmail(),
                        Fechas.iso(i.getFechaCreacion()), Fechas.iso(i.getFechaActualizacion()),
                        Fechas.iso(i.getFechaCierre())))
                .toList();
        List<RespaldoDTO.Evento> listaEventos = !conEventos ? List.of() : eventos.todos().stream()
                .map(e -> new RespaldoDTO.Evento(ref(e.getDispositivo()), e.getTipo().name(),
                        e.getEstadoResultante() == null ? null : e.getEstadoResultante().name(),
                        e.getOrigen().name(), Fechas.iso(e.getFechaHora()), Fechas.iso(e.getFechaFin()),
                        e.getDetalle()))
                .toList();
        return new RespaldoDTO(VERSION, Fechas.ahoraIso(), "ideacom IDS", config, listaSitios, deptos, listaUsuarios,
                listaDispositivos, listaIncidencias, listaEventos);
    }

    private static RespaldoDTO.RefDispositivo ref(Dispositivo d) {
        return d == null ? null : new RespaldoDTO.RefDispositivo(d.getSitio().getCodigoAlta(), d.getMac(),
                d.getNombre());
    }

    // ------------------------------------------------------------ restaurar

    public ResultadoRestauracionDTO restaurar(RespaldoDTO r, Sujeto quien) {
        if (r == null || r.version() == null) {
            throw ApiException.peticionInvalida("El archivo no es un respaldo de ideacom IDS");
        }
        if (r.version() > VERSION) {
            throw ApiException.peticionInvalida("El respaldo es de una versión más nueva del sistema");
        }
        List<Seccion> secciones = new ArrayList<>();
        Set<String> avisos = new LinkedHashSet<>();

        secciones.add(restaurarConfiguracion(r.configuracion()));
        secciones.add(restaurarSitios(r.sitios()));
        secciones.add(restaurarDepartamentos(r.departamentos()));
        secciones.add(restaurarUsuarios(r.usuarios(), quien, avisos));

        if (r.dispositivos() != null && !r.dispositivos().isEmpty()) {
            ResultadoImportacionDTO res = importacion.importar(r.dispositivos(), 1, true, true);
            secciones.add(new Seccion("Dispositivos", res.creados(), res.actualizados(), res.errores()));
            res.detalles().stream().filter(d -> "ERROR".equals(d.accion()))
                    .forEach(d -> avisos.add("Dispositivo '" + d.nombre() + "': " + d.mensaje()));
            avisos.addAll(res.avisos());
        } else {
            secciones.add(new Seccion("Dispositivos", 0, 0, 0));
        }
        secciones.add(restaurarIncidencias(r.incidencias(), quien, avisos));
        secciones.add(restaurarEventos(r.eventos()));
        return new ResultadoRestauracionDTO(secciones, List.copyOf(avisos));
    }

    private Seccion restaurarConfiguracion(List<RespaldoDTO.Config> lista) {
        int actualizados = 0;
        int omitidos = 0;
        for (RespaldoDTO.Config c : nulaComoVacia(lista)) {
            Optional<Configuracion> actual = c.clave() == null ? Optional.empty() : configuracion.buscar(c.clave());
            if (actual.isPresent() && c.valor() != null) {
                actual.get().setValor(c.valor());
                actualizados++;
            } else {
                omitidos++;
            }
        }
        return new Seccion("Configuración", 0, actualizados, omitidos);
    }

    private Seccion restaurarSitios(List<RespaldoDTO.Sitio> lista) {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        for (RespaldoDTO.Sitio s : nulaComoVacia(lista)) {
            if (Textos.vacio(s.codigoAlta()) || Textos.vacio(s.nombre())) {
                omitidos++;
                continue;
            }
            Optional<Sitio> actual = sitios.buscarActivoPorCodigo(s.codigoAlta())
                    .or(() -> sitios.buscarPorCodigoONombre(s.codigoAlta())
                            .filter(x -> x.getCodigoAlta().equals(s.codigoAlta())));
            Sitio sitio = actual.orElseGet(Sitio::new);
            if (sitio.getId() == null) {
                sitio.setCodigoAlta(s.codigoAlta());
                sitio.setFechaAlta(Fechas.ahoraUtc());
                creados++;
            } else {
                actualizados++;
            }
            sitio.setNombre(Textos.limpio(s.nombre(), 100));
            sitio.setDireccion(Textos.limpio(s.direccion(), 200));
            sitio.setActivo(s.activo() == null || s.activo());
            sitios.guardar(sitio);
        }
        return new Seccion("Sitios", creados, actualizados, omitidos);
    }

    private Seccion restaurarDepartamentos(List<String> lista) {
        int creados = 0;
        int omitidos = 0;
        for (String nombre : nulaComoVacia(lista)) {
            String n = Textos.limpio(nombre, 80);
            if (n == null || departamentos.buscarPorNombre(n).isPresent()) {
                omitidos++;
                continue;
            }
            Departamento d = new Departamento();
            d.setNombre(n);
            departamentos.guardar(d);
            creados++;
        }
        return new Seccion("Departamentos", creados, 0, omitidos);
    }

    private Seccion restaurarUsuarios(List<RespaldoDTO.Usuario> lista, Sujeto quien, Set<String> avisos) {
        int creados = 0;
        int actualizados = 0;
        int omitidos = 0;
        for (RespaldoDTO.Usuario u : nulaComoVacia(lista)) {
            if (Textos.vacio(u.email()) || Textos.vacio(u.nombre())) {
                omitidos++;
                continue;
            }
            String email = u.email().trim().toLowerCase(Locale.ROOT);
            Rol rol = usuarios.rolPorNombre(u.rol() == null ? Rol.USUARIO : u.rol().toUpperCase(Locale.ROOT))
                    .orElse(null);
            if (rol == null) {
                avisos.add("Usuario " + email + ": rol desconocido, se omitió");
                omitidos++;
                continue;
            }
            Optional<Usuario> actual = usuarios.buscarPorEmail(email);
            if (actual.isPresent()) {
                Usuario existente = actual.get();
                existente.setNombre(Textos.limpio(u.nombre(), 100));
                // Nunca se quita el acceso de quien esta restaurando
                if (!existente.getId().equals(quien.id())) {
                    existente.setRol(rol);
                    existente.setActivo(u.activo() == null || u.activo());
                }
                usuarios.guardar(existente);
                actualizados++;
            } else {
                if (u.passwordHash() == null || !u.passwordHash().startsWith("pbkdf2_sha256$")) {
                    avisos.add("Usuario " + email + ": sin contraseña válida en el respaldo, se omitió");
                    omitidos++;
                    continue;
                }
                Usuario nuevo = new Usuario();
                nuevo.setNombre(Textos.limpio(u.nombre(), 100));
                nuevo.setEmail(email);
                nuevo.setPasswordHash(u.passwordHash());
                nuevo.setRol(rol);
                nuevo.setActivo(u.activo() == null || u.activo());
                LocalDateTime alta = fecha(u.fechaAlta());
                nuevo.setFechaAlta(alta == null ? Fechas.ahoraUtc() : alta);
                usuarios.guardar(nuevo);
                creados++;
            }
        }
        return new Seccion("Usuarios", creados, actualizados, omitidos);
    }

    private Seccion restaurarIncidencias(List<RespaldoDTO.Incidencia> lista, Sujeto quien, Set<String> avisos) {
        int creados = 0;
        int omitidos = 0;
        Usuario restaurador = usuarios.buscarPorId(quien.id()).orElseThrow();
        for (RespaldoDTO.Incidencia r : nulaComoVacia(lista)) {
            LocalDateTime creada = fecha(r.fechaCreacion());
            if (Textos.vacio(r.titulo()) || creada == null) {
                omitidos++;
                continue;
            }
            Usuario creador = r.creadaPor() == null ? restaurador
                    : usuarios.buscarPorEmail(r.creadaPor()).orElse(restaurador);
            String titulo = Textos.limpio(r.titulo(), 150);
            if (incidencias.existe(titulo, creada, creador.getId())) {
                omitidos++;
                continue;
            }
            Incidencia i = new Incidencia();
            i.setTitulo(titulo);
            i.setDescripcion(r.descripcion());
            PrioridadIncidencia p = enumSeguro(PrioridadIncidencia.class, r.prioridad());
            i.setPrioridad(p == null ? PrioridadIncidencia.MEDIA : p);
            EstadoIncidencia e = enumSeguro(EstadoIncidencia.class, r.estado());
            i.setEstado(e == null ? EstadoIncidencia.ABIERTA : e);
            i.setDispositivo(buscar(r.dispositivo()).orElse(null));
            if (r.dispositivo() != null && i.getDispositivo() == null) {
                avisos.add("Incidencia '" + titulo + "': no se encontró su dispositivo");
            }
            i.setCreadaPor(creador);
            i.setAsignadaA(r.asignadaA() == null ? null : usuarios.buscarPorEmail(r.asignadaA()).orElse(null));
            i.setFechaCreacion(creada);
            LocalDateTime act = fecha(r.fechaActualizacion());
            i.setFechaActualizacion(act == null ? creada : act);
            i.setFechaCierre(fecha(r.fechaCierre()));
            incidencias.guardar(i);
            creados++;
        }
        return new Seccion("Incidencias", creados, 0, omitidos);
    }

    private Seccion restaurarEventos(List<RespaldoDTO.Evento> lista) {
        int creados = 0;
        int omitidos = 0;
        LocalDateTime ahora = Fechas.ahoraUtc();
        for (RespaldoDTO.Evento r : nulaComoVacia(lista)) {
            TipoEvento tipo = enumSeguro(TipoEvento.class, r.tipo());
            LocalDateTime cuando = fecha(r.fechaHora());
            Optional<Dispositivo> d = buscar(r.dispositivo());
            if (tipo == null || cuando == null || d.isEmpty() || eventos.existe(d.get().getId(), tipo, cuando)) {
                omitidos++;
                continue;
            }
            OrigenEvento origen = enumSeguro(OrigenEvento.class, r.origen());
            Evento e = new Evento(d.get(), tipo, enumSeguro(EstadoDispositivo.class, r.estadoResultante()),
                    origen == null ? OrigenEvento.SERVIDOR : origen, cuando, Textos.limpio(r.detalle(), 300));
            e.setFechaFin(fecha(r.fechaFin()));
            e.setFechaRegistro(ahora);
            eventos.guardar(e);
            creados++;
        }
        return new Seccion("Eventos", creados, 0, omitidos);
    }

    // ------------------------------------------------------------ auxiliares

    private Optional<Dispositivo> buscar(RespaldoDTO.RefDispositivo ref) {
        if (ref == null || Textos.vacio(ref.sitio())) {
            return Optional.empty();
        }
        Optional<Sitio> sitio = sitios.buscarPorCodigoONombre(ref.sitio());
        if (sitio.isEmpty()) {
            return Optional.empty();
        }
        if (!Textos.vacio(ref.mac())) {
            Optional<Dispositivo> porMac = dispositivos.buscarPorSitioYMac(sitio.get().getId(), ref.mac());
            if (porMac.isPresent()) {
                return porMac;
            }
        }
        return Textos.vacio(ref.nombre()) ? Optional.empty()
                : dispositivos.buscarPorSitioYNombre(sitio.get().getId(), ref.nombre());
    }

    private static LocalDateTime fecha(String iso) {
        try {
            return Fechas.parseUtc(iso);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static <E extends Enum<E>> E enumSeguro(Class<E> tipo, String valor) {
        try {
            return Textos.enumeracion(tipo, valor, "valor");
        } catch (ApiException e) {
            return null;
        }
    }

    private static <T> List<T> nulaComoVacia(List<T> lista) {
        return lista == null ? List.of() : lista;
    }
}
