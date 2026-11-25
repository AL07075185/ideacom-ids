package mx.ideacom.ids.common.reglas;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.TipoEvento;

/**
 * Maquina de estados del dispositivo. Es logica pura (sin base de datos) para
 * poder probarla con JUnit y reutilizarla en el servidor.
 */
public final class ReglasEstado {

    private ReglasEstado() {
    }

    /** Estado que resulta de recibir un latido. */
    public static EstadoDispositivo alRecibirLatido(boolean internetOk) {
        return internetOk ? EstadoDispositivo.EN_LINEA : EstadoDispositivo.SIN_INTERNET;
    }

    /**
     * Indica si el ultimo latido ya vencio: paso mas de intervalo x umbral.
     * Si nunca hubo latido devuelve false (el equipo sigue DESCONOCIDO).
     */
    public static boolean latidoVencido(LocalDateTime ultimoLatido, LocalDateTime ahora,
                                        int intervaloSegundos, int umbral) {
        if (ultimoLatido == null || ahora == null) {
            return false;
        }
        if (intervaloSegundos <= 0 || umbral <= 0) {
            throw new IllegalArgumentException("Intervalo y umbral deben ser mayores a cero");
        }
        Duration limite = Duration.ofSeconds((long) intervaloSegundos * umbral);
        return Duration.between(ultimoLatido, ahora).compareTo(limite) > 0;
    }

    /**
     * Evalua si un dispositivo debe pasar a DESCONECTADO por falta de latidos.
     * Solo aplica a estados que esperan latidos: un equipo APAGADO no se marca
     * como desconectado.
     */
    public static Optional<EstadoDispositivo> evaluarVencimiento(EstadoDispositivo actual,
                                                                 LocalDateTime ultimoLatido,
                                                                 LocalDateTime ahora,
                                                                 int intervaloSegundos,
                                                                 int umbral) {
        if (actual != null && actual.esperaLatidos()
                && latidoVencido(ultimoLatido, ahora, intervaloSegundos, umbral)) {
            return Optional.of(EstadoDispositivo.DESCONECTADO);
        }
        return Optional.empty();
    }

    /**
     * Tipo de evento que se registra al pasar de un estado a otro, o vacio si
     * el cambio no merece registro (por ejemplo, EN_LINEA a EN_LINEA).
     */
    public static Optional<TipoEvento> eventoDeTransicion(EstadoDispositivo anterior,
                                                          EstadoDispositivo nuevo) {
        if (nuevo == null || anterior == nuevo) {
            return Optional.empty();
        }
        switch (nuevo) {
            case DESCONECTADO:
                return Optional.of(TipoEvento.DESCONEXION_INESPERADA);
            case APAGADO:
                return Optional.of(TipoEvento.APAGADO);
            case EN_LINEA:
            case SIN_INTERNET:
                if (anterior == EstadoDispositivo.EN_LINEA) {
                    return Optional.of(TipoEvento.SIN_INTERNET);
                }
                if (anterior == EstadoDispositivo.SIN_INTERNET) {
                    return Optional.of(TipoEvento.INTERNET_RESTABLECIDO);
                }
                return Optional.of(TipoEvento.RECONEXION);
            default:
                return Optional.empty();
        }
    }

    /**
     * Indica si un apagado normal que el agente reporta despues (al reiniciar)
     * explica una desconexion inesperada. La desconexion se fecha en el ultimo
     * latido; el apagado debio ocurrir poco despues, antes de que el detector
     * la marcara (intervalo x umbral) mas un margen de un minuto.
     *
     * @param ultimoLatido fecha de la desconexion (ultimo latido recibido)
     * @param apagado      fecha del apagado normal que reporta el agente
     */
    public static boolean apagadoExplicaDesconexion(LocalDateTime ultimoLatido, LocalDateTime apagado,
                                                    int intervaloSegundos, int umbral) {
        if (ultimoLatido == null || apagado == null) {
            return false;
        }
        Duration margen = Duration.ofMinutes(1);
        Duration limite = Duration.ofSeconds((long) intervaloSegundos * umbral).plus(margen);
        Duration diferencia = Duration.between(ultimoLatido, apagado);
        // Se tolera hasta un minuto antes del ultimo latido por diferencias de reloj
        return diferencia.compareTo(margen.negated()) >= 0 && diferencia.compareTo(limite) <= 0;
    }
}
