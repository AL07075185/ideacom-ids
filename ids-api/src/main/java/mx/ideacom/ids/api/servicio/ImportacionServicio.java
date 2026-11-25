package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import mx.ideacom.ids.api.dto.FilaDispositivoDTO;
import mx.ideacom.ids.api.dto.ResultadoImportacionDTO;
import mx.ideacom.ids.api.dto.ResultadoImportacionDTO.Detalle;
import mx.ideacom.ids.api.entidad.Departamento;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.OrigenEvento;
import mx.ideacom.ids.api.entidad.Sitio;
import mx.ideacom.ids.api.entidad.TipoDispositivo;
import mx.ideacom.ids.api.entidad.Usuario;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DepartamentoRepositorio;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.repositorio.UsuarioRepositorio;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.util.Fechas;

/**
 * Importa dispositivos desde CSV o JSON. Evita duplicados: si ya existe un
 * equipo con la misma MAC en el sitio (o el mismo nombre, si no trae MAC), lo
 * actualiza en lugar de crearlo.
 */
@ApplicationScoped
@Transactional
public class ImportacionServicio {

    public static final int MAX_FILAS = 5000;

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private DepartamentoRepositorio departamentos;

    @Inject
    private UsuarioRepositorio usuarios;

    @Inject
    private EventoRepositorio eventos;

    /** Conexion "conectado a" que se resuelve cuando ya existen todos los equipos. */
    private record Conexion(Dispositivo dispositivo, Integer idSitio, String nombrePadre, int fila) {
    }

    /**
     * @param filas       renglones a importar
     * @param primeraFila numero con el que se muestra el primer renglon (2 en CSV por el encabezado)
     * @param aplicar     false = vista previa, no guarda nada
     * @param respaldo    true cuando viene de un respaldo: restaura tambien datos tecnicos y estado de registro
     */
    public ResultadoImportacionDTO importar(List<FilaDispositivoDTO> filas, int primeraFila, boolean aplicar,
                                            boolean respaldo) {
        if (filas == null || filas.isEmpty()) {
            throw ApiException.peticionInvalida("El archivo no tiene dispositivos");
        }
        if (filas.size() > MAX_FILAS) {
            throw ApiException.peticionInvalida("Máximo " + MAX_FILAS + " dispositivos por archivo");
        }
        List<Detalle> detalles = new ArrayList<>();
        Set<String> avisos = new LinkedHashSet<>();
        Set<String> vistos = new HashSet<>();
        Set<String> nombresEnArchivo = new HashSet<>();
        List<Conexion> conexiones = new ArrayList<>();
        int creados = 0;
        int actualizados = 0;
        int errores = 0;
        LocalDateTime ahora = Fechas.ahoraUtc();

        for (int i = 0; i < filas.size(); i++) {
            FilaDispositivoDTO f = filas.get(i);
            int numero = primeraFila + i;
            String nombre = f == null ? null : f.nombre();
            try {
                if (f == null) {
                    throw ApiException.peticionInvalida("Renglón vacío");
                }
                nombre = Textos.obligatorio(f.nombre(), 100, "nombre");
                Sitio sitio = resolverSitio(f.sitio());
                TipoDispositivo tipo = Textos.enumeracion(TipoDispositivo.class, f.tipo(), "tipo");
                String mac = Textos.mac(f.mac());
                boolean conAgente = Boolean.TRUE.equals(f.conAgente());
                String clave = sitio.getId() + "|" + (mac != null ? mac : "nombre:" + nombre.toLowerCase(Locale.ROOT));

                Optional<Dispositivo> existente = mac != null
                        ? dispositivos.buscarPorSitioYMac(sitio.getId(), mac)
                        : dispositivos.buscarPorSitioYNombre(sitio.getId(), nombre);
                boolean nuevo = existente.isEmpty() && !vistos.contains(clave);
                if (nuevo && conAgente && mac == null) {
                    throw ApiException.peticionInvalida("Un equipo con agente necesita su MAC para reconocerlo");
                }
                Departamento depto = resolverDepartamento(f.departamento(), aplicar, avisos);
                Usuario responsable = resolverResponsable(f.responsable(), avisos);

                if (aplicar) {
                    Dispositivo d = existente.orElseGet(() -> mac != null
                            ? dispositivos.buscarPorSitioYMac(sitio.getId(), mac).orElse(null)
                            : dispositivos.buscarPorSitioYNombre(sitio.getId(), f.nombre().trim()).orElse(null));
                    if (d == null) {
                        d = nuevoDispositivo(sitio, conAgente, f, respaldo, ahora);
                    }
                    d.setNombre(nombre);
                    if (tipo != null) {
                        d.setTipo(tipo);
                    }
                    if (f.ubicacion() != null) {
                        d.setUbicacion(Textos.limpio(f.ubicacion(), 120));
                    }
                    if (f.notas() != null) {
                        d.setNotas(Textos.limpio(f.notas(), 500));
                    }
                    if (depto != null) {
                        d.setDepartamento(depto);
                    }
                    if (responsable != null) {
                        d.setResponsable(responsable);
                    }
                    // Los datos de red de un equipo con agente los mantiene el agente
                    if (!d.isConAgente() || d.getId() == null) {
                        if (f.ip() != null) {
                            d.setIp(Textos.limpio(f.ip(), 45));
                        }
                        if (f.ipv6() != null) {
                            d.setIpv6(Textos.limpio(f.ipv6(), 45));
                        }
                        if (mac != null) {
                            d.setMac(mac);
                        }
                    }
                    boolean recienCreado = d.getId() == null;
                    d = dispositivos.guardar(d);
                    if (recienCreado) {
                        Evento e = new Evento(d, TipoEvento.REGISTRO, d.getEstadoActual(), OrigenEvento.PANEL, ahora,
                                respaldo ? "Restaurado desde un respaldo" : "Importado desde archivo");
                        e.setFechaRegistro(ahora);
                        eventos.guardar(e);
                    }
                    if (f.conectadoA() != null) {
                        conexiones.add(new Conexion(d, sitio.getId(), f.conectadoA().trim(), numero));
                    }
                } else if (f.conectadoA() != null) {
                    conexiones.add(new Conexion(null, sitio.getId(), f.conectadoA().trim(), numero));
                }
                vistos.add(clave);
                nombresEnArchivo.add(sitio.getId() + "|" + nombre.toLowerCase(Locale.ROOT));
                if (nuevo) {
                    creados++;
                } else {
                    actualizados++;
                }
                detalles.add(new Detalle(numero, nombre, nuevo ? "CREAR" : "ACTUALIZAR", null));
            } catch (ApiException | IllegalArgumentException e) {
                errores++;
                detalles.add(new Detalle(numero, nombre, "ERROR", e.getMessage()));
            }
        }

        resolverConexiones(conexiones, aplicar, nombresEnArchivo, avisos);
        return new ResultadoImportacionDTO(!aplicar, filas.size(), creados, actualizados, errores, detalles,
                List.copyOf(avisos));
    }

    private Dispositivo nuevoDispositivo(Sitio sitio, boolean conAgente, FilaDispositivoDTO f, boolean respaldo,
                                         LocalDateTime ahora) {
        Dispositivo d = new Dispositivo();
        d.setSitio(sitio);
        d.setConAgente(conAgente);
        d.setFechaAlta(ahora);
        EstadoRegistro registro = EstadoRegistro.APROBADO;
        if (respaldo && f.estadoRegistro() != null) {
            EstadoRegistro r = Textos.enumeracion(EstadoRegistro.class, f.estadoRegistro(), "estadoRegistro");
            registro = r == null ? EstadoRegistro.APROBADO : r;
        }
        d.setEstadoRegistro(registro);
        if (registro == EstadoRegistro.APROBADO) {
            d.setFechaAprobacion(ahora);
        }
        // Un equipo con agente queda "sin reportar" hasta que su agente se registre y lo reconozca por MAC
        d.setEstadoActual(conAgente ? EstadoDispositivo.DESCONOCIDO : EstadoDispositivo.SIN_AGENTE);
        if (respaldo && conAgente) {
            d.setHostname(Textos.limpio(f.hostname(), 100));
            d.setSoNombre(Textos.limpio(f.soNombre(), 80));
            d.setSoVersion(Textos.limpio(f.soVersion(), 80));
            d.setCpuModelo(Textos.limpio(f.cpuModelo(), 150));
            d.setRamMb(f.ramMb());
            d.setDiscoGb(f.discoGb());
        }
        return d;
    }

    private Sitio resolverSitio(String texto) {
        if (Textos.vacio(texto)) {
            List<Sitio> activos = sitios.listar().stream().filter(Sitio::isActivo).toList();
            if (activos.size() == 1) {
                return activos.get(0);
            }
            throw ApiException.peticionInvalida("Falta el sitio (hay más de un sitio registrado)");
        }
        return sitios.buscarPorCodigoONombre(texto.trim())
                .orElseThrow(() -> ApiException.peticionInvalida("No existe el sitio '" + texto.trim() + "'"));
    }

    private Departamento resolverDepartamento(String nombre, boolean aplicar, Set<String> avisos) {
        String n = Textos.limpio(nombre, 80);
        if (n == null) {
            return null;
        }
        Optional<Departamento> d = departamentos.buscarPorNombre(n);
        if (d.isPresent()) {
            return d.get();
        }
        avisos.add("Se creará el departamento '" + n + "'");
        if (!aplicar) {
            return null;
        }
        Departamento nuevo = new Departamento();
        nuevo.setNombre(n);
        return departamentos.guardar(nuevo);
    }

    private Usuario resolverResponsable(String email, Set<String> avisos) {
        String e = Textos.limpio(email, 150);
        if (e == null) {
            return null;
        }
        Optional<Usuario> u = usuarios.buscarPorEmail(e);
        if (u.isEmpty()) {
            avisos.add("No existe el usuario '" + e + "'; esos equipos quedarán sin responsable");
        }
        return u.orElse(null);
    }

    private void resolverConexiones(List<Conexion> conexiones, boolean aplicar, Set<String> nombresEnArchivo,
                                    Set<String> avisos) {
        for (Conexion c : conexiones) {
            Optional<Dispositivo> padre = dispositivos.buscarPorSitioYNombre(c.idSitio(), c.nombrePadre());
            boolean enArchivo = nombresEnArchivo.contains(c.idSitio() + "|" + c.nombrePadre().toLowerCase(Locale.ROOT));
            if (padre.isEmpty() && !(enArchivo && !aplicar)) {
                avisos.add("Fila " + c.fila() + ": no se encontró el equipo '" + c.nombrePadre()
                        + "' para 'conectado a'");
                continue;
            }
            if (aplicar && padre.isPresent() && !padre.get().getId().equals(c.dispositivo().getId())) {
                c.dispositivo().setConectadoA(padre.get());
                dispositivos.guardar(c.dispositivo());
            }
        }
    }
}
