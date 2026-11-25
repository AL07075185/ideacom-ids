package mx.ideacom.ids.common.reglas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;

/**
 * Calcula el porcentaje de disponibilidad de un dispositivo en una ventana de
 * tiempo, a partir de sus cambios de estado.
 *
 * <pre>
 * disponibilidad = tiempo disponible / (tiempo disponible + tiempo caido)
 * </pre>
 *
 * Un apagado avisado, el tiempo sin agente y el tiempo desconocido no cuentan
 * ni como disponible ni como caida: se excluyen del calculo.
 */
public final class DisponibilidadCalculadora {

    /** Un cambio de estado en un momento dado. */
    public record Cambio(LocalDateTime fecha, EstadoDispositivo estado) {
    }

    private DisponibilidadCalculadora() {
    }

    /** Tiempo acumulado por categoria dentro de una ventana. */
    public record Medicion(long disponibleMs, long caidaMs, long apagadoMs) {

        /** Porcentaje 0-100 con dos decimales, o vacio si no hubo tiempo medible. */
        public OptionalDouble porcentaje() {
            long medible = disponibleMs + caidaMs;
            if (medible == 0) {
                return OptionalDouble.empty();
            }
            return OptionalDouble.of(Math.round(disponibleMs * 10000.0 / medible) / 100.0);
        }

        public long minutosCaido() {
            return caidaMs / 60_000;
        }
    }

    /**
     * @param estadoInicial estado vigente al inicio de la ventana
     * @param cambios       cambios ordenados por fecha (los que caen fuera de la ventana se ignoran)
     * @param inicio        inicio de la ventana
     * @param fin           fin de la ventana
     * @return porcentaje 0-100, o vacio si no hubo tiempo medible
     */
    public static OptionalDouble calcular(EstadoDispositivo estadoInicial, List<Cambio> cambios,
                                          LocalDateTime inicio, LocalDateTime fin) {
        return medir(estadoInicial, cambios, inicio, fin).porcentaje();
    }

    /** Igual que {@link #calcular}, pero devuelve el detalle de tiempos. */
    public static Medicion medir(EstadoDispositivo estadoInicial, List<Cambio> cambios,
                                 LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null || !fin.isAfter(inicio)) {
            throw new IllegalArgumentException("La ventana de tiempo no es valida");
        }
        long[] acumulado = new long[3];
        EstadoDispositivo actual = estadoInicial == null ? EstadoDispositivo.DESCONOCIDO : estadoInicial;
        LocalDateTime desde = inicio;

        if (cambios != null) {
            for (Cambio c : cambios) {
                if (c == null || c.fecha() == null || c.estado() == null) {
                    continue;
                }
                if (!c.fecha().isAfter(inicio)) {
                    actual = c.estado();
                    continue;
                }
                if (c.fecha().isAfter(fin)) {
                    break;
                }
                sumar(acumulado, actual, Duration.between(desde, c.fecha()).toMillis());
                actual = c.estado();
                desde = c.fecha();
            }
        }
        sumar(acumulado, actual, Duration.between(desde, fin).toMillis());
        return new Medicion(acumulado[0], acumulado[1], acumulado[2]);
    }

    private static void sumar(long[] acumulado, EstadoDispositivo estado, long ms) {
        if (estado.cuentaComoDisponible()) {
            acumulado[0] += ms;
        } else if (estado.cuentaComoCaida()) {
            acumulado[1] += ms;
        } else if (estado == EstadoDispositivo.APAGADO) {
            acumulado[2] += ms;
        }
    }
}
