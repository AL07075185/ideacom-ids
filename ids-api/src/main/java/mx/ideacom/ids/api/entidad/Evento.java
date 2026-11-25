package mx.ideacom.ids.api.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import mx.ideacom.ids.common.modelo.EstadoDispositivo;
import mx.ideacom.ids.common.modelo.TipoEvento;

/** Cambio registrado en el historial de un dispositivo. */
@Entity
@Table(name = "evento")
public class Evento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_evento")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_dispositivo", nullable = false)
    private Dispositivo dispositivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 30)
    private TipoEvento tipo;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_resultante", length = 20)
    private EstadoDispositivo estadoResultante;

    @Enumerated(EnumType.STRING)
    @Column(name = "origen", nullable = false, length = 10)
    private OrigenEvento origen;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "detalle", length = 300)
    private String detalle;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    public Evento() {
    }

    public Evento(Dispositivo dispositivo, TipoEvento tipo, EstadoDispositivo estadoResultante,
                  OrigenEvento origen, LocalDateTime fechaHora, String detalle) {
        this.dispositivo = dispositivo;
        this.tipo = tipo;
        this.estadoResultante = estadoResultante;
        this.origen = origen;
        this.fechaHora = fechaHora;
        this.detalle = detalle;
    }

    public Long getId() {
        return id;
    }

    public Dispositivo getDispositivo() {
        return dispositivo;
    }

    public TipoEvento getTipo() {
        return tipo;
    }

    public EstadoDispositivo getEstadoResultante() {
        return estadoResultante;
    }

    public OrigenEvento getOrigen() {
        return origen;
    }

    public LocalDateTime getFechaHora() {
        return fechaHora;
    }

    /** Reclasifica el evento (por ejemplo, una desconexion que resulto ser un apagado normal). */
    public void reclasificar(TipoEvento nuevoTipo, EstadoDispositivo nuevoEstado, OrigenEvento nuevoOrigen,
                             String nuevoDetalle) {
        this.tipo = nuevoTipo;
        this.estadoResultante = nuevoEstado;
        this.origen = nuevoOrigen;
        this.detalle = nuevoDetalle;
    }

    public LocalDateTime getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDateTime fechaFin) {
        this.fechaFin = fechaFin;
    }

    public String getDetalle() {
        return detalle;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }
}
