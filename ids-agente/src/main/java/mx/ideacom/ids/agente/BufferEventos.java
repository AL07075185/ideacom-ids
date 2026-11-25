package mx.ideacom.ids.agente;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import mx.ideacom.ids.common.dto.EventoAgenteDTO;

/**
 * Cola de eventos pendientes de enviar, guardada en disco (un JSON por linea)
 * para no perderlos si no hay conexion o el equipo se reinicia.
 */
public final class BufferEventos {

    private static final Logger LOG = Logger.getLogger(BufferEventos.class.getName());
    private static final String ARCHIVO = "eventos-pendientes.jsonl";
    private static final int MAXIMO = 500;

    private final Path archivo;

    public BufferEventos(Path datosDir) throws IOException {
        Files.createDirectories(datosDir);
        this.archivo = datosDir.resolve(ARCHIVO);
    }

    public synchronized void agregar(EventoAgenteDTO evento) {
        try {
            List<EventoAgenteDTO> actuales = leer();
            if (actuales.size() >= MAXIMO) {
                LOG.warning("Buffer lleno: se descarta el evento mas antiguo");
                actuales.remove(0);
                reescribir(actuales);
            }
            Files.writeString(archivo, Json.escribir(evento) + System.lineSeparator(), StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo guardar el evento pendiente", e);
        }
    }

    public synchronized List<EventoAgenteDTO> leer() {
        List<EventoAgenteDTO> lista = new ArrayList<>();
        if (!Files.exists(archivo)) {
            return lista;
        }
        try {
            for (String linea : Files.readAllLines(archivo, StandardCharsets.UTF_8)) {
                if (linea.isBlank()) {
                    continue;
                }
                try {
                    Map<String, Object> m = Json.leerObjeto(linea);
                    lista.add(new EventoAgenteDTO(Json.texto(m, "tipo"), Json.texto(m, "fechaHora"),
                            Json.texto(m, "fechaFin"), Json.texto(m, "detalle")));
                } catch (IllegalArgumentException e) {
                    LOG.warning("Linea invalida en el buffer, se ignora");
                }
            }
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo leer el buffer de eventos", e);
        }
        return lista;
    }

    /** Quita los primeros {@code n} eventos (los que ya se enviaron). */
    public synchronized void quitarPrimeros(int n) {
        List<EventoAgenteDTO> actuales = leer();
        try {
            reescribir(actuales.subList(Math.min(n, actuales.size()), actuales.size()));
        } catch (IOException e) {
            LOG.log(Level.WARNING, "No se pudo limpiar el buffer de eventos", e);
        }
    }

    public synchronized boolean vacio() {
        return leer().isEmpty();
    }

    private void reescribir(List<EventoAgenteDTO> eventos) throws IOException {
        if (eventos.isEmpty()) {
            Files.deleteIfExists(archivo);
            return;
        }
        StringBuilder sb = new StringBuilder();
        for (EventoAgenteDTO e : eventos) {
            sb.append(Json.escribir(e)).append(System.lineSeparator());
        }
        Files.writeString(archivo, sb.toString(), StandardCharsets.UTF_8);
    }
}
