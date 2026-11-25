package mx.ideacom.ids.common.modelo;

/**
 * Tipos de evento que se registran en el historial de un dispositivo.
 * Solo se guardan cambios; los latidos normales no generan eventos.
 */
public enum TipoEvento {
    REGISTRO,
    APROBADO,
    RECHAZADO,
    ARRANQUE,
    APAGADO,
    RECONEXION,
    DESCONEXION_INESPERADA,
    SIN_INTERNET,
    INTERNET_RESTABLECIDO,
    SIN_CONEXION_SERVIDOR,
    CAMBIO_IP;

    /** Eventos que el agente tiene permitido enviar por /agentes/eventos. */
    public boolean permitidoDesdeAgente() {
        return this == ARRANQUE || this == APAGADO || this == SIN_CONEXION_SERVIDOR;
    }
}
