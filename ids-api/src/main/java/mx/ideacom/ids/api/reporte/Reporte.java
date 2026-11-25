package mx.ideacom.ids.api.reporte;

import java.util.List;

/**
 * Modelo del reporte de disponibilidad para la vista JSP.
 * Se usan JavaBeans con getters (no records) porque EL los lee como propiedades:
 * ${reporte.promedio}, ${fila.nombre}...
 */
public class Reporte {

    /** Opcion del filtro de sitios. */
    public static class OpcionSitio {
        private final Integer id;
        private final String nombre;

        public OpcionSitio(Integer id, String nombre) {
            this.id = id;
            this.nombre = nombre;
        }

        public Integer getId() {
            return id;
        }

        public String getNombre() {
            return nombre;
        }
    }

    /** Un renglon de la tabla de dispositivos. */
    public static class Fila {
        private final String nombre;
        private final String tipo;
        private final String ip;
        private final String sitio;
        private final String departamento;
        private final String estado;
        private final Double disponibilidad;
        private final long desconexiones;
        private final String tiempoCaido;
        private final String ultimoReporte;

        public Fila(String nombre, String tipo, String ip, String sitio, String departamento, String estado,
                    Double disponibilidad, long desconexiones, String tiempoCaido, String ultimoReporte) {
            this.nombre = nombre;
            this.tipo = tipo;
            this.ip = ip;
            this.sitio = sitio;
            this.departamento = departamento;
            this.estado = estado;
            this.disponibilidad = disponibilidad;
            this.desconexiones = desconexiones;
            this.tiempoCaido = tiempoCaido;
            this.ultimoReporte = ultimoReporte;
        }

        public String getNombre() {
            return nombre;
        }

        public String getTipo() {
            return tipo;
        }

        public String getIp() {
            return ip;
        }

        public String getSitio() {
            return sitio;
        }

        public String getDepartamento() {
            return departamento;
        }

        public String getEstado() {
            return estado;
        }

        public Double getDisponibilidad() {
            return disponibilidad;
        }

        public long getDesconexiones() {
            return desconexiones;
        }

        public String getTiempoCaido() {
            return tiempoCaido;
        }

        public String getUltimoReporte() {
            return ultimoReporte;
        }
    }

    /** Incidencia abierta o en proceso. */
    public static class IncidenciaAbierta {
        private final Integer id;
        private final String titulo;
        private final String prioridad;
        private final String estado;
        private final String dispositivo;
        private final String fecha;

        public IncidenciaAbierta(Integer id, String titulo, String prioridad, String estado, String dispositivo,
                                 String fecha) {
            this.id = id;
            this.titulo = titulo;
            this.prioridad = prioridad;
            this.estado = estado;
            this.dispositivo = dispositivo;
            this.fecha = fecha;
        }

        public Integer getId() {
            return id;
        }

        public String getTitulo() {
            return titulo;
        }

        public String getPrioridad() {
            return prioridad;
        }

        public String getEstado() {
            return estado;
        }

        public String getDispositivo() {
            return dispositivo;
        }

        public String getFecha() {
            return fecha;
        }
    }

    /** Evento de falla o apagado dentro del periodo. */
    public static class EventoRelevante {
        private final String fecha;
        private final String dispositivo;
        private final String evento;
        private final String estado;
        private final String detalle;

        public EventoRelevante(String fecha, String dispositivo, String evento, String estado, String detalle) {
            this.fecha = fecha;
            this.dispositivo = dispositivo;
            this.evento = evento;
            this.estado = estado;
            this.detalle = detalle;
        }

        public String getFecha() {
            return fecha;
        }

        public String getDispositivo() {
            return dispositivo;
        }

        public String getEvento() {
            return evento;
        }

        public String getEstado() {
            return estado;
        }

        public String getDetalle() {
            return detalle;
        }
    }

    private String generado;
    private String zona;
    private String usuario;
    private int horas;
    private String periodo;
    private Integer idSitio;
    private String sitio;
    private List<OpcionSitio> sitios = List.of();
    private List<Fila> filas = List.of();
    private List<IncidenciaAbierta> incidencias = List.of();
    private List<EventoRelevante> eventos = List.of();
    private Double promedio;
    private long totalDesconexiones;
    private String tiempoCaidoTotal;

    public int getTotalEquipos() {
        return filas.size();
    }

    public String getGenerado() {
        return generado;
    }

    public void setGenerado(String generado) {
        this.generado = generado;
    }

    public String getZona() {
        return zona;
    }

    public void setZona(String zona) {
        this.zona = zona;
    }

    public String getUsuario() {
        return usuario;
    }

    public void setUsuario(String usuario) {
        this.usuario = usuario;
    }

    public int getHoras() {
        return horas;
    }

    public void setHoras(int horas) {
        this.horas = horas;
    }

    public String getPeriodo() {
        return periodo;
    }

    public void setPeriodo(String periodo) {
        this.periodo = periodo;
    }

    public Integer getIdSitio() {
        return idSitio;
    }

    public void setIdSitio(Integer idSitio) {
        this.idSitio = idSitio;
    }

    public String getSitio() {
        return sitio;
    }

    public void setSitio(String sitio) {
        this.sitio = sitio;
    }

    public List<OpcionSitio> getSitios() {
        return sitios;
    }

    public void setSitios(List<OpcionSitio> sitios) {
        this.sitios = sitios;
    }

    public List<Fila> getFilas() {
        return filas;
    }

    public void setFilas(List<Fila> filas) {
        this.filas = filas;
    }

    public List<IncidenciaAbierta> getIncidencias() {
        return incidencias;
    }

    public void setIncidencias(List<IncidenciaAbierta> incidencias) {
        this.incidencias = incidencias;
    }

    public List<EventoRelevante> getEventos() {
        return eventos;
    }

    public void setEventos(List<EventoRelevante> eventos) {
        this.eventos = eventos;
    }

    public Double getPromedio() {
        return promedio;
    }

    public void setPromedio(Double promedio) {
        this.promedio = promedio;
    }

    public long getTotalDesconexiones() {
        return totalDesconexiones;
    }

    public void setTotalDesconexiones(long totalDesconexiones) {
        this.totalDesconexiones = totalDesconexiones;
    }

    public String getTiempoCaidoTotal() {
        return tiempoCaidoTotal;
    }

    public void setTiempoCaidoTotal(String tiempoCaidoTotal) {
        this.tiempoCaidoTotal = tiempoCaidoTotal;
    }
}
