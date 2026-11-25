package mx.ideacom.ids.agente;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Properties;

/**
 * Guarda el token y el id que entrega el servidor al registrarse, en
 * agente-estado.properties dentro de datos.dir. Asi el agente no se vuelve a
 * registrar en cada reinicio.
 */
public final class EstadoLocal {

    private static final String ARCHIVO = "agente-estado.properties";

    private final Path archivo;
    private Integer dispositivoId;
    private String token;
    private Instant ultimoContacto;
    private Instant apagadoAvisado;

    public EstadoLocal(Path datosDir) throws IOException {
        Files.createDirectories(datosDir);
        this.archivo = datosDir.resolve(ARCHIVO);
        if (Files.exists(archivo)) {
            Properties p = new Properties();
            try (InputStream in = Files.newInputStream(archivo)) {
                p.load(in);
            }
            this.token = p.getProperty("token");
            String id = p.getProperty("dispositivo.id");
            this.dispositivoId = id == null ? null : Integer.valueOf(id);
            String contacto = p.getProperty("ultimo.contacto");
            this.ultimoContacto = contacto == null ? null : Instant.parse(contacto);
            String avisado = p.getProperty("apagado.avisado");
            this.apagadoAvisado = avisado == null ? null : Instant.parse(avisado);
        }
    }

    public synchronized boolean tieneToken() {
        return token != null && !token.isBlank();
    }

    public synchronized String token() {
        return token;
    }

    public synchronized Integer dispositivoId() {
        return dispositivoId;
    }

    public synchronized void guardar(Integer id, String nuevoToken) throws IOException {
        this.dispositivoId = id;
        this.token = nuevoToken;
        escribir();
    }

    /** Ultima vez que el servidor respondio un latido (sobrevive a reinicios). */
    public synchronized Instant ultimoContacto() {
        return ultimoContacto;
    }

    public synchronized void registrarContacto(Instant momento) throws IOException {
        this.ultimoContacto = momento;
        escribir();
    }

    /** Momento en que el agente logro avisar un apagado al servidor (para no repetirlo al reiniciar). */
    public synchronized Instant apagadoAvisado() {
        return apagadoAvisado;
    }

    public synchronized void registrarApagadoAvisado(Instant momento) throws IOException {
        this.apagadoAvisado = momento;
        escribir();
    }

    public synchronized void borrarToken() throws IOException {
        this.token = null;
        escribir();
    }

    private void escribir() throws IOException {
        Properties p = new Properties();
        if (token != null) {
            p.setProperty("token", token);
        }
        if (dispositivoId != null) {
            p.setProperty("dispositivo.id", dispositivoId.toString());
        }
        if (ultimoContacto != null) {
            p.setProperty("ultimo.contacto", ultimoContacto.toString());
        }
        if (apagadoAvisado != null) {
            p.setProperty("apagado.avisado", apagadoAvisado.toString());
        }
        Path tmp = archivo.resolveSibling(ARCHIVO + ".tmp");
        try (OutputStream out = Files.newOutputStream(tmp)) {
            p.store(out, "ideacom IDS - no compartir: contiene el token del agente");
        }
        Files.move(tmp, archivo, StandardCopyOption.REPLACE_EXISTING);
    }
}
