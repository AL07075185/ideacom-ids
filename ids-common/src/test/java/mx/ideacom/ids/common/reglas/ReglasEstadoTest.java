package mx.ideacom.ids.common.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.TipoEvento;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ReglasEstadoTest {

    private static final LocalDateTime T0 = LocalDateTime.of(2026, 11, 20, 12, 0, 0);

    @Test
    @DisplayName("Un latido con internet deja el equipo EN_LINEA; sin internet, SIN_INTERNET")
    void latidoDefineEstado() {
        assertEquals(EstadoDispositivo.EN_LINEA, ReglasEstado.alRecibirLatido(true));
        assertEquals(EstadoDispositivo.SIN_INTERNET, ReglasEstado.alRecibirLatido(false));
    }

    @Test
    @DisplayName("El latido vence solo despues de intervalo x umbral")
    void latidoVencido() {
        // 60 s x 3 = 180 s
        assertFalse(ReglasEstado.latidoVencido(T0, T0.plusSeconds(180), 60, 3));
        assertTrue(ReglasEstado.latidoVencido(T0, T0.plusSeconds(181), 60, 3));
    }

    @Test
    @DisplayName("Sin latido previo nunca se considera vencido")
    void sinLatidoPrevio() {
        assertFalse(ReglasEstado.latidoVencido(null, T0, 60, 3));
    }

    @Test
    @DisplayName("Intervalo o umbral invalido lanza excepcion")
    void parametrosInvalidos() {
        assertThrows(IllegalArgumentException.class,
                () -> ReglasEstado.latidoVencido(T0, T0.plusMinutes(10), 0, 3));
    }

    @Test
    @DisplayName("Un equipo EN_LINEA con latido vencido pasa a DESCONECTADO")
    void enLineaVencidoSeDesconecta() {
        Optional<EstadoDispositivo> r = ReglasEstado.evaluarVencimiento(
                EstadoDispositivo.EN_LINEA, T0, T0.plusMinutes(5), 60, 3);
        assertEquals(Optional.of(EstadoDispositivo.DESCONECTADO), r);
    }

    @Test
    @DisplayName("Un equipo APAGADO no se marca como desconectado")
    void apagadoNoSeDesconecta() {
        assertTrue(ReglasEstado.evaluarVencimiento(
                EstadoDispositivo.APAGADO, T0, T0.plusHours(5), 60, 3).isEmpty());
    }

    @Test
    @DisplayName("Transiciones generan el evento correcto")
    void eventosDeTransicion() {
        assertEquals(Optional.of(TipoEvento.RECONEXION), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.DESCONECTADO, EstadoDispositivo.EN_LINEA));
        assertEquals(Optional.of(TipoEvento.RECONEXION), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.APAGADO, EstadoDispositivo.SIN_INTERNET));
        assertEquals(Optional.of(TipoEvento.SIN_INTERNET), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.EN_LINEA, EstadoDispositivo.SIN_INTERNET));
        assertEquals(Optional.of(TipoEvento.INTERNET_RESTABLECIDO), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.SIN_INTERNET, EstadoDispositivo.EN_LINEA));
        assertEquals(Optional.of(TipoEvento.DESCONEXION_INESPERADA), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.EN_LINEA, EstadoDispositivo.DESCONECTADO));
        assertEquals(Optional.of(TipoEvento.APAGADO), ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.EN_LINEA, EstadoDispositivo.APAGADO));
    }

    @Test
    @DisplayName("Sin cambio de estado no se registra evento")
    void sinCambioSinEvento() {
        assertTrue(ReglasEstado.eventoDeTransicion(
                EstadoDispositivo.EN_LINEA, EstadoDispositivo.EN_LINEA).isEmpty());
    }

    @Test
    @DisplayName("Un apagado poco despues del ultimo latido explica la desconexion")
    void apagadoExplicaDesconexion() {
        assertTrue(ReglasEstado.apagadoExplicaDesconexion(T0, T0.plusSeconds(40), 60, 3));
        assertTrue(ReglasEstado.apagadoExplicaDesconexion(T0, T0.minusSeconds(30), 60, 3));
        // Despues de 3 intervalos + 1 min ya no corresponde a esa desconexion
        assertFalse(ReglasEstado.apagadoExplicaDesconexion(T0, T0.plusMinutes(5), 60, 3));
        assertFalse(ReglasEstado.apagadoExplicaDesconexion(T0, T0.minusMinutes(5), 60, 3));
        assertFalse(ReglasEstado.apagadoExplicaDesconexion(null, T0, 60, 3));
    }
}
