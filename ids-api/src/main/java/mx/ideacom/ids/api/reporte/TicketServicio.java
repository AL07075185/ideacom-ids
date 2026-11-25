package mx.ideacom.ids.api.reporte;

import jakarta.enterprise.context.ApplicationScoped;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import mx.ideacom.ids.api.seguridad.Sujeto;
import mx.ideacom.ids.api.seguridad.Tokens;

/**
 * Tickets de un solo uso para abrir el reporte JSP en una pestana nueva.
 * El panel se autentica con un encabezado Authorization, que un enlace normal
 * no puede enviar; el ticket (valido 60 s) cubre ese paso.
 */
@ApplicationScoped
public class TicketServicio {

    private static final long VIGENCIA_SEGUNDOS = 60;

    private record Pendiente(Sujeto sujeto, Instant expira) {
    }

    private final Map<String, Pendiente> pendientes = new ConcurrentHashMap<>();

    public String crear(Sujeto sujeto) {
        Instant ahora = Instant.now();
        pendientes.values().removeIf(p -> p.expira().isBefore(ahora));
        String ticket = Tokens.nuevoToken();
        pendientes.put(ticket, new Pendiente(sujeto, ahora.plusSeconds(VIGENCIA_SEGUNDOS)));
        return ticket;
    }

    /** Devuelve el sujeto y elimina el ticket (solo se puede usar una vez). */
    public Optional<Sujeto> canjear(String ticket) {
        if (ticket == null) {
            return Optional.empty();
        }
        Pendiente p = pendientes.remove(ticket);
        if (p == null || p.expira().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(p.sujeto());
    }
}
