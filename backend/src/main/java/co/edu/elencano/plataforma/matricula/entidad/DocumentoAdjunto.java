package co.edu.elencano.plataforma.matricula.entidad;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Archivo subido para un tipo de documento de la solicitud. El archivo vive fuera de la base de datos. */
@Entity
@Table(name = "documento_adjunto")
public class DocumentoAdjunto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudMatricula solicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tipo_documento_id", nullable = false)
    private TipoDocumentoRequerido tipoDocumento;

    @Column(name = "nombre_original", nullable = false, length = 200)
    private String nombreOriginal;

    @Column(name = "ruta_almacenamiento", nullable = false, length = 300)
    private String rutaAlmacenamiento;

    @Column(name = "tipo_mime", nullable = false, length = 50)
    private String tipoMime;

    @Column(name = "tamano_bytes", nullable = false)
    private long tamanoBytes;

    @Column(name = "fecha_carga", nullable = false)
    private LocalDateTime fechaCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoDocumento estado = EstadoDocumento.PENDIENTE;

    @Column(length = 300)
    private String observacion;

    protected DocumentoAdjunto() {
    }

    DocumentoAdjunto(SolicitudMatricula solicitud, TipoDocumentoRequerido tipoDocumento) {
        this.solicitud = solicitud;
        this.tipoDocumento = tipoDocumento;
    }

    /** Registra el archivo nuevo. Vuelve a quedar pendiente de revision. */
    public void reemplazarArchivo(String nombreOriginal, String ruta, String tipoMime, long tamano, LocalDateTime fecha) {
        this.nombreOriginal = nombreOriginal;
        this.rutaAlmacenamiento = ruta;
        this.tipoMime = tipoMime;
        this.tamanoBytes = tamano;
        this.fechaCarga = fecha;
        this.estado = EstadoDocumento.PENDIENTE;
        this.observacion = null;
    }

    public void revisar(EstadoDocumento estado, String observacion) {
        this.estado = estado;
        this.observacion = observacion;
    }

    public Long getId() {
        return id;
    }

    public SolicitudMatricula getSolicitud() {
        return solicitud;
    }

    public TipoDocumentoRequerido getTipoDocumento() {
        return tipoDocumento;
    }

    public String getNombreOriginal() {
        return nombreOriginal;
    }

    public String getRutaAlmacenamiento() {
        return rutaAlmacenamiento;
    }

    public String getTipoMime() {
        return tipoMime;
    }

    public long getTamanoBytes() {
        return tamanoBytes;
    }

    public LocalDateTime getFechaCarga() {
        return fechaCarga;
    }

    public EstadoDocumento getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }
}
