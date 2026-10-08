package co.edu.elencano.plataforma.asistencia.entidad;

import java.time.LocalDateTime;

import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
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

/** Estado de un estudiante (por su matricula) en una clase. */
@Entity
@Table(name = "detalle_asistencia")
public class DetalleAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registro_id", nullable = false)
    private RegistroAsistencia registro;

    @ManyToOne(optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoAsistencia estado = EstadoAsistencia.ASISTIO;

    @Column(length = 300)
    private String observacion;

    @Column(length = 500)
    private String justificacion;

    @Column(name = "fecha_justificacion")
    private LocalDateTime fechaJustificacion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "justificado_por")
    private Usuario justificadoPor;

    protected DetalleAsistencia() {
    }

    DetalleAsistencia(RegistroAsistencia registro, Matricula matricula) {
        this.registro = registro;
        this.matricula = matricula;
    }

    /**
     * Cambia el estado que marca el docente. Una falta ya justificada no se pierde si el docente
     * vuelve a guardar la clase con esa falta; si la cambia a otro estado, la justificacion se borra.
     */
    public void marcar(EstadoAsistencia nuevo, String observacion) {
        this.observacion = observacion;
        boolean sigueSiendoFalta = nuevo == EstadoAsistencia.FALTA || nuevo == EstadoAsistencia.FALTA_JUSTIFICADA;
        if (estado == EstadoAsistencia.FALTA_JUSTIFICADA && sigueSiendoFalta) {
            return;
        }
        estado = nuevo;
        justificacion = null;
        fechaJustificacion = null;
        justificadoPor = null;
    }

    public void justificar(String justificacion, Usuario usuario, LocalDateTime ahora) {
        this.estado = EstadoAsistencia.FALTA_JUSTIFICADA;
        this.justificacion = justificacion;
        this.justificadoPor = usuario;
        this.fechaJustificacion = ahora;
    }

    public Long getId() {
        return id;
    }

    public RegistroAsistencia getRegistro() {
        return registro;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public EstadoAsistencia getEstado() {
        return estado;
    }

    public String getObservacion() {
        return observacion;
    }

    public String getJustificacion() {
        return justificacion;
    }

    public LocalDateTime getFechaJustificacion() {
        return fechaJustificacion;
    }
}
