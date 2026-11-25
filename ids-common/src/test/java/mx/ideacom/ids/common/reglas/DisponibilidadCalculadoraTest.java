package mx.ideacom.ids.common.reglas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.OptionalDouble;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.reglas.DisponibilidadCalculadora.Cambio;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class DisponibilidadCalculadoraTest {

    private static final LocalDateTime INICIO = LocalDateTime.of(2026, 11, 20, 0, 0);
    private static final LocalDateTime FIN = INICIO.plusHours(10);

    @Test
    @DisplayName("Todo el tiempo en linea da 100%")
    void todoEnLinea() {
        OptionalDouble r = DisponibilidadCalculadora.calcular(
                EstadoDispositivo.EN_LINEA, List.of(), INICIO, FIN);
        assertEquals(100.0, r.getAsDouble());
    }

    @Test
    @DisplayName("1 hora desconectado de 10 da 90%")
    void unaHoraCaido() {
        List<Cambio> cambios = List.of(
                new Cambio(INICIO.plusHours(2), EstadoDispositivo.DESCONECTADO),
                new Cambio(INICIO.plusHours(3), EstadoDispositivo.EN_LINEA));
        assertEquals(90.0, DisponibilidadCalculadora.calcular(
                EstadoDispositivo.EN_LINEA, cambios, INICIO, FIN).getAsDouble());
    }

    @Test
    @DisplayName("El tiempo apagado se excluye: 5 h apagado y 1 h caido de 10 da 80%")
    void apagadoNoCuentaComoCaida() {
        List<Cambio> cambios = List.of(
                new Cambio(INICIO.plusHours(1), EstadoDispositivo.APAGADO),
                new Cambio(INICIO.plusHours(6), EstadoDispositivo.EN_LINEA),
                new Cambio(INICIO.plusHours(8), EstadoDispositivo.DESCONECTADO),
                new Cambio(INICIO.plusHours(9), EstadoDispositivo.SIN_INTERNET));
        // disponible: 1 + 2 + 1 = 4 h ; caido: 1 h -> 4/5 = 80 %
        assertEquals(80.0, DisponibilidadCalculadora.calcular(
                EstadoDispositivo.EN_LINEA, cambios, INICIO, FIN).getAsDouble());
    }

    @Test
    @DisplayName("Cambios anteriores a la ventana solo fijan el estado inicial")
    void cambiosPreviosFijanEstado() {
        List<Cambio> cambios = List.of(
                new Cambio(INICIO.minusHours(1), EstadoDispositivo.DESCONECTADO),
                new Cambio(INICIO.plusHours(5), EstadoDispositivo.EN_LINEA));
        assertEquals(50.0, DisponibilidadCalculadora.calcular(
                EstadoDispositivo.EN_LINEA, cambios, INICIO, FIN).getAsDouble());
    }

    @Test
    @DisplayName("Sin tiempo medible (todo apagado) devuelve vacio")
    void sinTiempoMedible() {
        assertTrue(DisponibilidadCalculadora.calcular(
                EstadoDispositivo.APAGADO, List.of(), INICIO, FIN).isEmpty());
    }

    @Test
    @DisplayName("Ventana invalida lanza excepcion")
    void ventanaInvalida() {
        assertThrows(IllegalArgumentException.class, () -> DisponibilidadCalculadora.calcular(
                EstadoDispositivo.EN_LINEA, List.of(), FIN, INICIO));
    }

    @Test
    @DisplayName("La medicion separa tiempo disponible, caido y apagado")
    void medicionDetallada() {
        List<Cambio> cambios = List.of(
                new Cambio(INICIO.plusHours(1), EstadoDispositivo.APAGADO),
                new Cambio(INICIO.plusHours(6), EstadoDispositivo.EN_LINEA),
                new Cambio(INICIO.plusHours(8), EstadoDispositivo.DESCONECTADO),
                new Cambio(INICIO.plusHours(9), EstadoDispositivo.EN_LINEA));
        DisponibilidadCalculadora.Medicion m = DisponibilidadCalculadora.medir(
                EstadoDispositivo.EN_LINEA, cambios, INICIO, FIN);
        assertEquals(4 * 3_600_000L, m.disponibleMs());
        assertEquals(60L, m.minutosCaido());
        assertEquals(5 * 3_600_000L, m.apagadoMs());
        assertEquals(80.0, m.porcentaje().getAsDouble());
    }
}
