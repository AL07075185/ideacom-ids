package mx.ideacom.ids.api.dto;

import mx.ideacom.ids.api.entidad.Evento;
import mx.ideacom.ids.common.util.Fechas;

public record EventoDTO(Long id, Integer idDispositivo, String dispositivo, String tipo, String estadoResultante,
                        String origen, String fechaHora, String fechaFin, String detalle) {

    public static EventoDTO de(Evento e) {
        return new EventoDTO(e.getId(), e.getDispositivo().getId(), e.getDispositivo().getNombre(),
                e.getTipo().name(),
                e.getEstadoResultante() == null ? null : e.getEstadoResultante().name(),
                e.getOrigen().name(), Fechas.iso(e.getFechaHora()), Fechas.iso(e.getFechaFin()), e.getDetalle());
    }
}
