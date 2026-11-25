package mx.ideacom.ids.api.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import mx.ideacom.ids.api.entidad.Dispositivo;
import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.api.entidad.OrigenEvento;
import mx.ideacom.ids.api.entidad.Sitio;
import mx.ideacom.ids.api.entidad.TipoDispositivo;
import mx.ideacom.ids.api.excepcion.ApiException;
import mx.ideacom.ids.api.repositorio.DispositivoRepositorio;
import mx.ideacom.ids.api.repositorio.EventoRepositorio;
import mx.ideacom.ids.api.repositorio.SitioRepositorio;
import mx.ideacom.ids.api.seguridad.Tokens;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;
import mx.ideacom.ids.common.dto.LatidoRequest;
import mx.ideacom.ids.common.dto.LatidoResponse;
import mx.ideacom.ids.common.dto.RegistroAgenteRequest;
import mx.ideacom.ids.common.dto.RegistroAgenteResponse;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.EstadoRegistro;
import mx.ideacom.ids.common.modelo.TipoEvento;
import mx.ideacom.ids.common.reglas.ReglasEstado;
import mx.ideacom.ids.common.util.Fechas;

/** Logica de lo que reportan los agentes: alta automatica, latidos y eventos. */
@ApplicationScoped
@Transactional
public class AgenteServicio {

    private static final int MAX_EVENTOS_POR_LOTE = 500;

    @Inject
    private SitioRepositorio sitios;

    @Inject
    private DispositivoRepositorio dispositivos;

    @Inject
    private EventoRepositorio eventos;

    @Inject
    private ConfiguracionServicio configuracion;

    /**
     * Alta automatica. Si el equipo (misma MAC en el mismo sitio) ya existia,
     * actualiza sus datos tecnicos y emite un token nuevo.
     */
    public RegistroAgenteResponse registrar(RegistroAgenteRequest req) {
        if (req == null) {
            throw ApiException.peticionInvalida("Cuerpo vacío");
        }
        String codigo = Textos.obligatorio(req.codigoAlta(), 40, "codigoAlta");
        String mac = Textos.mac(Textos.obligatorio(req.mac(), 30, "mac"));
        String hostname = Textos.obligatorio(req.hostname(), 100, "hostname");

        Sitio sitio = sitios.buscarActivoPorCodigo(codigo)
                .orElseThrow(() -> ApiException.prohibido("Código de alta inválido o sitio inactivo"));
        LocalDateTime ahora = Fechas.ahoraUtc();

        Dispositivo d = dispositivos.buscarPorSitioYMac(sitio.getId(), mac).orElse(null);
        boolean nuevo = d == null;
        if (nuevo) {
            d = new Dispositivo();
            d.setSitio(sitio);
            d.setMac(mac);
            d.setNombre(hostname);
            d.setTipo(deducirTipo(req.soNombre()));
            d.setConAgente(true);
            d.setEstadoRegistro(EstadoRegistro.PENDIENTE);
            d.setEstadoActual(EstadoDispositivo.DESCONOCIDO);
            d.setFechaAlta(ahora);
        } else if (d.getEstadoRegistro() == EstadoRegistro.RECHAZADO) {
            throw ApiException.prohibido("El equipo fue rechazado por el administrador");
        } else if (!d.isConAgente()) {
            // Equipo importado o dado de alta a mano: el agente lo "adopta" y
            // conserva su aprobacion y sus datos administrativos.
            d.setConAgente(true);
            d.setEstadoActual(EstadoDispositivo.DESCONOCIDO);
        }
        aplicarDatosTecnicos(d, req, hostname);
        String token = Tokens.nuevoToken();
        d.setTokenHash(Tokens.sha256(token));
        d = dispositivos.guardar(d);

        registrarEvento(d, TipoEvento.REGISTRO, null, OrigenEvento.AGENTE, ahora,
                nuevo ? "Alta automática desde " + hostname : "Nuevo registro del agente en " + hostname);

        return new RegistroAgenteResponse(d.getId(), token, configuracion.intervaloSegundos(),
                d.getEstadoRegistro().name());
    }

    /** Procesa un latido: actualiza el estado y registra el evento si hubo cambio. */
    public LatidoResponse latido(Integer idDispositivo, LatidoRequest req) {
        Dispositivo d = buscar(idDispositivo);
        LocalDateTime ahora = Fechas.ahoraUtc();
        boolean internetOk = req == null || req.internetOk() == null || req.internetOk();

        String ipNueva = req == null ? null : Textos.limpio(req.ip(), 45);
        if (ipNueva != null && d.getIp() != null && !Objects.equals(ipNueva, d.getIp())) {
            registrarEvento(d, TipoEvento.CAMBIO_IP, null, OrigenEvento.SERVIDOR, ahora,
                    d.getIp() + " -> " + ipNueva);
        }
        if (ipNueva != null) {
            d.setIp(ipNueva);
        }
        if (req != null) {
            d.setIpv6(Textos.limpio(req.ipv6(), 45));
        }
        if (req != null && req.versionAgente() != null) {
            d.setVersionAgente(Textos.limpio(req.versionAgente(), 20));
        }

        EstadoDispositivo anterior = d.getEstadoActual();
        EstadoDispositivo nuevo = ReglasEstado.alRecibirLatido(internetOk);
        ReglasEstado.eventoDeTransicion(anterior, nuevo).ifPresent(tipo ->
                registrarEvento(d, tipo, nuevo, OrigenEvento.SERVIDOR, ahora, null));
        d.setEstadoActual(nuevo);
        d.setInternetOk(internetOk);
        d.setUltimoLatido(ahora);
        dispositivos.guardar(d);

        return new LatidoResponse(configuracion.intervaloSegundos(), d.getEstadoRegistro().name());
    }

    /** Eventos que el agente reporta: ARRANQUE, APAGADO y SIN_CONEXION_SERVIDOR. */
    public void eventos(Integer idDispositivo, List<EventoAgenteDTO> lote) {
        if (lote == null || lote.isEmpty()) {
            return;
        }
        if (lote.size() > MAX_EVENTOS_POR_LOTE) {
            throw ApiException.peticionInvalida("Máximo " + MAX_EVENTOS_POR_LOTE + " eventos por envío");
        }
        Dispositivo d = buscar(idDispositivo);
        LocalDateTime ahora = Fechas.ahoraUtc();
        for (EventoAgenteDTO dto : lote) {
            if (dto == null || Textos.vacio(dto.tipo())) {
                continue;
            }
            TipoEvento tipo = Textos.enumeracion(TipoEvento.class, dto.tipo(), "tipo");
            if (!tipo.permitidoDesdeAgente()) {
                throw ApiException.peticionInvalida("El agente no puede reportar eventos " + tipo);
            }
            LocalDateTime fecha = Fechas.parseUtc(dto.fechaHora());
            if (fecha == null || fecha.isAfter(ahora)) {
                fecha = ahora;
            }
            String detalle = Textos.limpio(dto.detalle(), 300);
            switch (tipo) {
                case APAGADO -> procesarApagado(d, fecha, detalle);
                case SIN_CONEXION_SERVIDOR -> {
                    Evento e = new Evento(d, tipo, null, OrigenEvento.AGENTE, fecha,
                            detalle == null ? "Sin comunicación con el servidor" : detalle);
                    e.setFechaFin(Fechas.parseUtc(dto.fechaFin()));
                    guardarEvento(e);
                }
                default -> registrarEvento(d, tipo, null, OrigenEvento.AGENTE, fecha,
                        detalle == null ? "Agente iniciado" : detalle);
            }
        }
        dispositivos.guardar(d);
    }

    private void procesarApagado(Dispositivo d, LocalDateTime fecha, String detalle) {
        // Un aviso de apagado que llega tarde (guardado sin conexion) no debe
        // cambiar el estado si despues ya hubo latidos.
        boolean vigente = d.getUltimoLatido() == null || !fecha.isBefore(d.getUltimoLatido());
        if (vigente) {
            registrarEvento(d, TipoEvento.APAGADO, EstadoDispositivo.APAGADO, OrigenEvento.AGENTE, fecha,
                    detalle == null ? "Apagado ordenado del equipo" : detalle);
            d.setEstadoActual(EstadoDispositivo.APAGADO);
        } else if (!reclasificarDesconexion(d, fecha, detalle)) {
            registrarEvento(d, TipoEvento.APAGADO, null, OrigenEvento.AGENTE, fecha,
                    detalle == null ? "Aviso de apagado recibido después (sin conexión en ese momento)" : detalle);
        }
    }

    /**
     * Un apagado normal reportado al reiniciar (por ejemplo, detectado en el
     * registro de eventos de Windows) convierte la desconexion inesperada que lo
     * acompana en un APAGADO, para que no cuente como caida.
     */
    private boolean reclasificarDesconexion(Dispositivo d, LocalDateTime apagado, String detalle) {
        Optional<Evento> desconexion = eventos.ultimaDesconexionHasta(d.getId(), apagado.plusMinutes(1));
        if (desconexion.isEmpty() || !ReglasEstado.apagadoExplicaDesconexion(desconexion.get().getFechaHora(),
                apagado, configuracion.intervaloSegundos(), configuracion.umbral())) {
            return false;
        }
        desconexion.get().reclasificar(TipoEvento.APAGADO, EstadoDispositivo.APAGADO, OrigenEvento.AGENTE,
                (detalle == null ? "Apagado normal" : detalle) + " (confirmado al reiniciar)");
        if (d.getEstadoActual() == EstadoDispositivo.DESCONECTADO) {
            d.setEstadoActual(EstadoDispositivo.APAGADO);
        }
        return true;
    }

    private Dispositivo buscar(Integer id) {
        Dispositivo d = dispositivos.buscarPorId(id)
                .orElseThrow(() -> ApiException.noAutenticado("Dispositivo no encontrado"));
        if (d.getEstadoRegistro() == EstadoRegistro.RECHAZADO) {
            throw ApiException.prohibido("El equipo fue rechazado por el administrador");
        }
        return d;
    }

    private void aplicarDatosTecnicos(Dispositivo d, RegistroAgenteRequest r, String hostname) {
        d.setHostname(hostname);
        d.setIp(Textos.limpio(r.ip(), 45));
        d.setIpv6(Textos.limpio(r.ipv6(), 45));
        d.setSoNombre(Textos.limpio(r.soNombre(), 80));
        d.setSoVersion(Textos.limpio(r.soVersion(), 80));
        d.setArquitectura(Textos.limpio(r.arquitectura(), 30));
        d.setCpuModelo(Textos.limpio(r.cpuModelo(), 150));
        d.setRamMb(r.ramMb());
        d.setDiscoGb(r.discoGb());
        d.setVersionAgente(Textos.limpio(r.versionAgente(), 20));
    }

    private static TipoDispositivo deducirTipo(String soNombre) {
        if (soNombre != null && soNombre.toLowerCase(Locale.ROOT).contains("server")) {
            return TipoDispositivo.SERVIDOR;
        }
        return TipoDispositivo.PC;
    }

    private void registrarEvento(Dispositivo d, TipoEvento tipo, EstadoDispositivo estado, OrigenEvento origen,
                                 LocalDateTime fecha, String detalle) {
        guardarEvento(new Evento(d, tipo, estado, origen, fecha, detalle));
    }

    private void guardarEvento(Evento e) {
        e.setFechaRegistro(Fechas.ahoraUtc());
        eventos.guardar(e);
    }
}
