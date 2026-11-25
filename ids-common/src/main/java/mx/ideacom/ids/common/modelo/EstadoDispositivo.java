package mx.ideacom.ids.common.modelo;

/**
 * Estado operativo de un dispositivo, visto desde el servidor.
 */
public enum EstadoDispositivo {
    /** Reporta latidos y tiene salida a internet. */
    EN_LINEA,
    /** Reporta latidos al servidor, pero su prueba de internet falla. */
    SIN_INTERNET,
    /** El agente aviso un apagado ordenado. */
    APAGADO,
    /** Dejo de reportar sin avisar (corte de luz, cable, red caida). */
    DESCONECTADO,
    /** Equipo registrado a mano (switch, router, camara); no tiene agente. */
    SIN_AGENTE,
    /** Registrado, pero aun no envia su primer latido. */
    DESCONOCIDO;

    /** Tiempo que suma como disponible en el calculo de disponibilidad. */
    public boolean cuentaComoDisponible() {
        return this == EN_LINEA || this == SIN_INTERNET;
    }

    /** Tiempo que suma como caida en el calculo de disponibilidad. */
    public boolean cuentaComoCaida() {
        return this == DESCONECTADO;
    }

    /** Estados en los que el detector espera recibir latidos. */
    public boolean esperaLatidos() {
        return this == EN_LINEA || this == SIN_INTERNET;
    }
}
