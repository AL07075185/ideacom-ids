package mx.ideacom.ids.api.dto;

import mx.ideacom.ids.api.entidad.Incidencia;
import mx.ideacom.ids.common.util.Fechas;

public record IncidenciaDTO(Integer id, String titulo, String descripcion, String prioridad, String estado,
                            Integer idDispositivo, String dispositivo, Integer idCreadaPor, String creadaPor,
                            Integer idAsignadaA, String asignadaA, String fechaCreacion,
                            String fechaActualizacion, String fechaCierre) {

    public static IncidenciaDTO de(Incidencia i) {
        return new IncidenciaDTO(i.getId(), i.getTitulo(), i.getDescripcion(), i.getPrioridad().name(),
                i.getEstado().name(),
                i.getDispositivo() == null ? null : i.getDispositivo().getId(),
                i.getDispositivo() == null ? null : i.getDispositivo().getNombre(),
                i.getCreadaPor().getId(), i.getCreadaPor().getNombre(),
                i.getAsignadaA() == null ? null : i.getAsignadaA().getId(),
                i.getAsignadaA() == null ? null : i.getAsignadaA().getNombre(),
                Fechas.iso(i.getFechaCreacion()), Fechas.iso(i.getFechaActualizacion()),
                Fechas.iso(i.getFechaCierre()));
    }
}
