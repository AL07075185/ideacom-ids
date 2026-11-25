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
import mx.ideacom.ids.common.modelo.EstadoRegistro;

/**
 * Equipo monitoreado. Tiene dos grupos de campos:
 * <ul>
 *   <li>Administrativos: los edita el administrador (nombre, tipo, departamento, responsable...).</li>
 *   <li>Tecnicos: los actualiza el agente (SO, hardware, IP, MAC...).</li>
 * </ul>
 */
@Entity
@Table(name = "dispositivo")
public class Dispositivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dispositivo")
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_sitio", nullable = false)
    private Sitio sitio;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_departamento")
    private Departamento departamento;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_responsable")
    private Usuario responsable;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_conectado_a")
    private Dispositivo conectadoA;

    // ----- Administrativos -----
    @Column(name = "nombre", nullable = false, length = 100)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoDispositivo tipo = TipoDispositivo.PC;

    @Column(name = "ubicacion", length = 120)
    private String ubicacion;

    @Column(name = "notas", length = 500)
    private String notas;

    // ----- Tecnicos -----
    @Column(name = "con_agente", nullable = false)
    private boolean conAgente;

    @Column(name = "hostname", length = 100)
    private String hostname;

    @Column(name = "ip", length = 45)
    private String ip;

    @Column(name = "ipv6", length = 45)
    private String ipv6;

    @Column(name = "mac", length = 17)
    private String mac;

    @Column(name = "so_nombre", length = 80)
    private String soNombre;

    @Column(name = "so_version", length = 80)
    private String soVersion;

    @Column(name = "arquitectura", length = 30)
    private String arquitectura;

    @Column(name = "cpu_modelo", length = 150)
    private String cpuModelo;

    @Column(name = "ram_mb")
    private Long ramMb;

    @Column(name = "disco_gb")
    private Long discoGb;

    @Column(name = "version_agente", length = 20)
    private String versionAgente;

    @Column(name = "token_hash", length = 64, unique = true)
    private String tokenHash;

    // ----- Estado -----
    @Enumerated(EnumType.STRING)
    @Column(name = "estado_registro", nullable = false, length = 15)
    private EstadoRegistro estadoRegistro = EstadoRegistro.PENDIENTE;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_actual", nullable = false, length = 20)
    private EstadoDispositivo estadoActual = EstadoDispositivo.DESCONOCIDO;

    @Column(name = "internet_ok")
    private Boolean internetOk;

    @Column(name = "ultimo_latido")
    private LocalDateTime ultimoLatido;

    @Column(name = "fecha_alta", nullable = false)
    private LocalDateTime fechaAlta;

    @Column(name = "fecha_aprobacion")
    private LocalDateTime fechaAprobacion;

    public Integer getId() {
        return id;
    }

    public Sitio getSitio() {
        return sitio;
    }

    public void setSitio(Sitio sitio) {
        this.sitio = sitio;
    }

    public Departamento getDepartamento() {
        return departamento;
    }

    public void setDepartamento(Departamento departamento) {
        this.departamento = departamento;
    }

    public Usuario getResponsable() {
        return responsable;
    }

    public void setResponsable(Usuario responsable) {
        this.responsable = responsable;
    }

    public Dispositivo getConectadoA() {
        return conectadoA;
    }

    public void setConectadoA(Dispositivo conectadoA) {
        this.conectadoA = conectadoA;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public TipoDispositivo getTipo() {
        return tipo;
    }

    public void setTipo(TipoDispositivo tipo) {
        this.tipo = tipo;
    }

    public String getUbicacion() {
        return ubicacion;
    }

    public void setUbicacion(String ubicacion) {
        this.ubicacion = ubicacion;
    }

    public String getNotas() {
        return notas;
    }

    public void setNotas(String notas) {
        this.notas = notas;
    }

    public boolean isConAgente() {
        return conAgente;
    }

    public void setConAgente(boolean conAgente) {
        this.conAgente = conAgente;
    }

    public String getHostname() {
        return hostname;
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getIpv6() {
        return ipv6;
    }

    public void setIpv6(String ipv6) {
        this.ipv6 = ipv6;
    }

    public String getMac() {
        return mac;
    }

    public void setMac(String mac) {
        this.mac = mac;
    }

    public String getSoNombre() {
        return soNombre;
    }

    public void setSoNombre(String soNombre) {
        this.soNombre = soNombre;
    }

    public String getSoVersion() {
        return soVersion;
    }

    public void setSoVersion(String soVersion) {
        this.soVersion = soVersion;
    }

    public String getArquitectura() {
        return arquitectura;
    }

    public void setArquitectura(String arquitectura) {
        this.arquitectura = arquitectura;
    }

    public String getCpuModelo() {
        return cpuModelo;
    }

    public void setCpuModelo(String cpuModelo) {
        this.cpuModelo = cpuModelo;
    }

    public Long getRamMb() {
        return ramMb;
    }

    public void setRamMb(Long ramMb) {
        this.ramMb = ramMb;
    }

    public Long getDiscoGb() {
        return discoGb;
    }

    public void setDiscoGb(Long discoGb) {
        this.discoGb = discoGb;
    }

    public String getVersionAgente() {
        return versionAgente;
    }

    public void setVersionAgente(String versionAgente) {
        this.versionAgente = versionAgente;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public EstadoRegistro getEstadoRegistro() {
        return estadoRegistro;
    }

    public void setEstadoRegistro(EstadoRegistro estadoRegistro) {
        this.estadoRegistro = estadoRegistro;
    }

    public EstadoDispositivo getEstadoActual() {
        return estadoActual;
    }

    public void setEstadoActual(EstadoDispositivo estadoActual) {
        this.estadoActual = estadoActual;
    }

    public Boolean getInternetOk() {
        return internetOk;
    }

    public void setInternetOk(Boolean internetOk) {
        this.internetOk = internetOk;
    }

    public LocalDateTime getUltimoLatido() {
        return ultimoLatido;
    }

    public void setUltimoLatido(LocalDateTime ultimoLatido) {
        this.ultimoLatido = ultimoLatido;
    }

    public LocalDateTime getFechaAlta() {
        return fechaAlta;
    }

    public void setFechaAlta(LocalDateTime fechaAlta) {
        this.fechaAlta = fechaAlta;
    }

    public LocalDateTime getFechaAprobacion() {
        return fechaAprobacion;
    }

    public void setFechaAprobacion(LocalDateTime fechaAprobacion) {
        this.fechaAprobacion = fechaAprobacion;
    }
}
