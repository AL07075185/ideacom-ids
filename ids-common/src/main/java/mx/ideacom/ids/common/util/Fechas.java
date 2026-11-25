package mx.ideacom.ids.common.util;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Utilidades de fecha. Todo el sistema trabaja en UTC; el panel convierte a la
 * hora local del navegador.
 */
public final class Fechas {

    private Fechas() {
    }

    /** Hora actual en UTC, truncada a milisegundos (precision de DATETIME(3)). */
    public static LocalDateTime ahoraUtc() {
        return LocalDateTime.now(ZoneOffset.UTC).truncatedTo(ChronoUnit.MILLIS);
    }

    /** Convierte una fecha UTC a texto ISO-8601 con zona Z, o null. */
    public static String iso(LocalDateTime utc) {
        return utc == null ? null : utc.toInstant(ZoneOffset.UTC).toString();
    }

    /** Hora actual en texto ISO-8601. */
    public static String ahoraIso() {
        return Instant.now().truncatedTo(ChronoUnit.MILLIS).toString();
    }

    /**
     * Lee un texto ISO-8601. Acepta "2026-11-20T15:04:05Z", con offset
     * ("...-06:00") o sin zona (se asume UTC). Devuelve null si el texto es nulo.
     *
     * @throws IllegalArgumentException si el formato no es valido
     */
    public static LocalDateTime parseUtc(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String t = texto.trim();
        try {
            return LocalDateTime.ofInstant(Instant.parse(t), ZoneOffset.UTC)
                    .truncatedTo(ChronoUnit.MILLIS);
        } catch (DateTimeParseException e) {
            try {
                return java.time.OffsetDateTime.parse(t)
                        .withOffsetSameInstant(ZoneOffset.UTC)
                        .toLocalDateTime().truncatedTo(ChronoUnit.MILLIS);
            } catch (DateTimeParseException e2) {
                try {
                    return LocalDateTime.parse(t).truncatedTo(ChronoUnit.MILLIS);
                } catch (DateTimeParseException e3) {
                    throw new IllegalArgumentException("Fecha invalida: " + texto, e3);
                }
            }
        }
    }
}
