package mx.ideacom.ids.agente;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.util.List;

/**
 * Prueba la salida a internet abriendo una conexion TCP a hosts conocidos.
 * Basta con que uno responda. No usa ICMP (ping), que requiere privilegios.
 */
public final class RevisorInternet {

    private final List<ConfigAgente.Destino> destinos;
    private final int timeoutMs;

    public RevisorInternet(List<ConfigAgente.Destino> destinos, int timeoutMs) {
        this.destinos = destinos;
        this.timeoutMs = Math.min(timeoutMs, 3000);
    }

    public boolean hayInternet() {
        for (ConfigAgente.Destino d : destinos) {
            try (Socket s = new Socket()) {
                s.connect(new InetSocketAddress(d.host(), d.puerto()), timeoutMs);
                return true;
            } catch (IOException e) {
                // probar el siguiente destino
            }
        }
        return destinos.isEmpty();
    }
}
