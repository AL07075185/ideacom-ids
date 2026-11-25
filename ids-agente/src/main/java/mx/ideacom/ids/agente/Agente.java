package mx.ideacom.ids.agente;

import java.io.IOException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import mx.ideacom.ids.common.dto.LatidoRequest;
import mx.ideacom.ids.common.dto.LatidoResponse;
import mx.ideacom.ids.common.dto.RegistroAgenteRequest;
import mx.ideacom.ids.common.dto.RegistroAgenteResponse;
import mx.ideacom.ids.common.modelo.TipoEvento;

/**
 * Ciclo principal del agente:
 * <ol>
 *   <li>Si no tiene token, se registra con el codigo de alta del sitio.</li>
 *   <li>Cada intervalo envia un latido con su IP y si tiene internet.</li>
 *   <li>Si pierde la comunicacion, guarda desde cuando y lo reporta al volver.</li>
 *   <li>Al apagarse el equipo, avisa APAGADO (shutdown hook).</li>
 * </ol>
 */
public final class Agente {

    private static final Logger LOG = Logger.getLogger(Agente.class.getName());
    private static final int ESPERA_TRAS_RECHAZO_S = 300;

    private final ConfigAgente config;
    private final EstadoLocal estado;
    private final BufferEventos buffer;
    private final ClienteApi cliente;
    private final RevisorInternet revisor;
    private final Recolector recolector;
    private final ScheduledExecutorService planificador;

    private volatile int intervaloSegundos;
    private volatile boolean apagando;
    private Instant sinConexionDesde;
    private Motivo motivoCorte;

    /** Causa de un periodo sin comunicacion, de menor a mayor gravedad. */
    enum Motivo {
        API("API no disponible"),
        SIN_INTERNET("Sin internet"),
        SIN_RED("Sin red local");

        private final String texto;

        Motivo(String texto) {
            this.texto = texto;
        }

        String texto() {
            return texto;
        }
    }

    public Agente(ConfigAgente config) throws IOException {
        this.config = config;
        this.estado = new EstadoLocal(config.datosDir());
        this.buffer = new BufferEventos(config.datosDir());
        this.cliente = new ClienteApi(config.apiUrl(), config.timeoutMs());
        this.revisor = new RevisorInternet(config.hostsInternet(), config.timeoutMs());
        this.recolector = new Recolector(config.apiUrl());
        this.intervaloSegundos = config.intervaloSegundos();
        this.planificador = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "ids-agente");
            t.setDaemon(false);
            return t;
        });
    }

    public void iniciar() {
        LOG.info("ideacom IDS agente " + ConfigAgente.VERSION + " iniciando. API: " + config.apiUrl());
        String nota = revisarApagadoAnterior();
        buffer.agregar(new EventoAgenteDTO(TipoEvento.ARRANQUE.name(), ahora(), null,
                "Agente " + ConfigAgente.VERSION + " iniciado" + (nota == null ? "" : ". " + nota)));
        Runtime.getRuntime().addShutdownHook(new Thread(this::alApagar, "ids-agente-apagado"));
        planificar(0);
    }

    /**
     * En Windows, el agente no alcanza a avisar el apagado del equipo. Al arrancar
     * revisa el registro de eventos: si hubo un apagado normal despues del ultimo
     * contacto con el servidor, lo reporta con su fecha real (el servidor convierte
     * esa desconexion en APAGADO). Si fue inesperado, devuelve una nota para el
     * evento de arranque.
     */
    private String revisarApagadoAnterior() {
        Instant contacto = estado.ultimoContacto();
        if (contacto == null || !ApagadoWindows.esWindows()) {
            return null;
        }
        Instant avisado = estado.apagadoAvisado();
        boolean yaAvisado = (avisado != null && !avisado.isBefore(contacto))
                || buffer.leer().stream().anyMatch(e -> TipoEvento.APAGADO.name().equals(e.tipo())
                && e.fechaHora() != null && Instant.parse(e.fechaHora()).isAfter(contacto));
        if (yaAvisado) {
            return null;
        }
        ApagadoWindows.Apagado anterior = ApagadoWindows.ultimoApagado().orElse(null);
        if (anterior == null || !anterior.fecha().isAfter(contacto)) {
            return null;
        }
        if (anterior.tipo() == ApagadoWindows.Tipo.NORMAL) {
            LOG.info("Windows registro un apagado normal; se reportara al servidor");
            buffer.agregar(new EventoAgenteDTO(TipoEvento.APAGADO.name(),
                    anterior.fecha().truncatedTo(ChronoUnit.MILLIS).toString(), null,
                    "Apagado normal de Windows (detectado al reiniciar)"));
            return null;
        }
        LOG.info("Windows registro un apagado inesperado de la sesion anterior");
        return "El apagado anterior fue inesperado (corte de energía o apagado forzado)";
    }

    private void planificar(int segundos) {
        if (!apagando) {
            planificador.schedule(this::ciclo, segundos, TimeUnit.SECONDS);
        }
    }

    /** Un ciclo: registrar si hace falta, latido y envio de pendientes. */
    void ciclo() {
        int siguiente = intervaloSegundos;
        try {
            if (!estado.tieneToken()) {
                registrar();
            }
            if (estado.tieneToken()) {
                latido();
                enviarPendientes();
            }
        } catch (ClienteApi.AccesoDenegadoException e) {
            LOG.severe("Acceso denegado por el servidor: " + e.getMessage()
                    + ". Revisa codigo.alta o pide al administrador aprobar el equipo.");
            siguiente = Math.max(intervaloSegundos, ESPERA_TRAS_RECHAZO_S);
        } catch (RuntimeException e) {
            LOG.log(Level.WARNING, "Error inesperado en el ciclo del agente", e);
        } finally {
            planificar(siguiente);
        }
    }

    private void registrar() throws ClienteApi.AccesoDenegadoException {
        Recolector.InfoEquipo info = recolector.recolectar();
        if (info.mac() == null) {
            LOG.severe("No se encontro una interfaz de red con MAC; no es posible registrarse");
            return;
        }
        RegistroAgenteRequest req = new RegistroAgenteRequest(config.codigoAlta(), info.hostname(), info.ip(),
                info.ipv6(), info.mac(), info.soNombre(), info.soVersion(), info.arquitectura(), info.cpuModelo(),
                info.ramMb(), info.discoGb(), ConfigAgente.VERSION);
        try {
            RegistroAgenteResponse r = cliente.registrar(req);
            estado.guardar(r.dispositivoId(), r.token());
            ajustarIntervalo(r.intervaloSegundos());
            LOG.info("Registrado como dispositivo " + r.dispositivoId() + " (" + r.estadoRegistro() + ")"
                    + ("PENDIENTE".equals(r.estadoRegistro()) ? " - esperando aprobacion en el panel" : ""));
        } catch (ClienteApi.AccesoDenegadoException e) {
            throw e;
        } catch (IOException e) {
            LOG.warning("No se pudo registrar (" + describir(e) + "). Se reintentara.");
        }
    }

    private void latido() throws ClienteApi.AccesoDenegadoException {
        boolean internet = revisor.hayInternet();
        LatidoRequest req = new LatidoRequest(recolector.ipActual(), recolector.ipv6Actual(), internet,
                ConfigAgente.VERSION);
        try {
            LatidoResponse r = cliente.latido(estado.token(), req);
            ajustarIntervalo(r.intervaloSegundos());
            guardarContacto();
            if (sinConexionDesde != null) {
                Instant fin = Instant.now();
                String motivo = motivoCorte.texto();
                buffer.agregar(new EventoAgenteDTO(TipoEvento.SIN_CONEXION_SERVIDOR.name(),
                        sinConexionDesde.truncatedTo(ChronoUnit.MILLIS).toString(),
                        fin.truncatedTo(ChronoUnit.MILLIS).toString(),
                        motivo + " durante " + Duration.between(sinConexionDesde, fin).toMinutes() + " min"));
                LOG.info("Comunicacion restablecida con el servidor");
                sinConexionDesde = null;
            }
        } catch (ClienteApi.TokenInvalidoException e) {
            LOG.warning("El servidor rechazo el token; el agente se registrara de nuevo");
            borrarToken();
        } catch (ClienteApi.AccesoDenegadoException e) {
            throw e;
        } catch (IOException e) {
            Motivo motivo = !recolector.hayRedLocal() ? Motivo.SIN_RED
                    : (!internet ? Motivo.SIN_INTERNET : Motivo.API);
            if (sinConexionDesde == null) {
                sinConexionDesde = Instant.now();
                motivoCorte = motivo;
                LOG.warning("Sin comunicacion con el servidor (" + describir(e) + "). Causa probable: "
                        + motivo.texto());
            } else if (motivo.ordinal() > motivoCorte.ordinal()) {
                motivoCorte = motivo;
            }
        }
    }

    private void guardarContacto() {
        try {
            estado.registrarContacto(Instant.now().truncatedTo(ChronoUnit.SECONDS));
        } catch (IOException e) {
            LOG.fine("No se pudo guardar la hora del ultimo contacto: " + e.getMessage());
        }
    }

    private void enviarPendientes() {
        List<EventoAgenteDTO> pendientes = buffer.leer();
        if (pendientes.isEmpty() || sinConexionDesde != null) {
            return;
        }
        try {
            cliente.eventos(estado.token(), pendientes, null);
            buffer.quitarPrimeros(pendientes.size());
        } catch (ClienteApi.TokenInvalidoException e) {
            borrarToken();
        } catch (IOException e) {
            LOG.fine("Eventos pendientes se enviaran despues: " + describir(e));
        }
    }

    /** Se ejecuta al apagar el equipo o detener el servicio. */
    void alApagar() {
        apagando = true;
        planificador.shutdownNow();
        EventoAgenteDTO apagado = new EventoAgenteDTO(TipoEvento.APAGADO.name(), ahora(), null,
                "Apagado ordenado del equipo");
        if (estado.tieneToken()) {
            List<EventoAgenteDTO> lote = new java.util.ArrayList<>(buffer.leer());
            lote.add(apagado);
            try {
                // Plazo corto: el sistema no espera mucho al cerrar procesos
                cliente.eventos(estado.token(), lote, Duration.ofSeconds(3));
                buffer.quitarPrimeros(lote.size() - 1);
                estado.registrarApagadoAvisado(Instant.now());
                LOG.info("Aviso de apagado enviado");
                return;
            } catch (IOException e) {
                LOG.warning("No se pudo avisar el apagado; se enviara al reiniciar");
            }
        }
        buffer.agregar(apagado);
    }

    private void ajustarIntervalo(Integer servidor) {
        if (servidor != null && servidor >= 10 && servidor != intervaloSegundos) {
            LOG.info("Intervalo de latido ajustado por el servidor a " + servidor + " s");
            intervaloSegundos = servidor;
        }
    }

    private void borrarToken() {
        try {
            estado.borrarToken();
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo borrar el token local", e);
        }
    }

    private static String ahora() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    }

    private static String describir(IOException e) {
        if (e instanceof HttpTimeoutException) {
            return "tiempo de espera agotado";
        }
        String m = e.getMessage();
        return e.getClass().getSimpleName() + (m == null ? "" : ": " + m);
    }
}
