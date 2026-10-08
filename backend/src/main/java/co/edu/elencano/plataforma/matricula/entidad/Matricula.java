package co.edu.elencano.plataforma.matricula.entidad;

import java.time.LocalDate;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
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

/**
 * Matricula de un estudiante en un anio lectivo. Hay una por estudiante y anio;
 * si el estudiante se retira y vuelve el mismo anio, se reactiva la misma.
 */
@Entity
@Table(name = "matricula")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id", nullable = false)
    private Estudiante estudiante;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anio_lectivo_id", nullable = false)
    private AnioLectivo anioLectivo;

    @ManyToOne
    @JoinColumn(name = "grupo_id")
    private Grupo grupo;

    @Column(name = "fecha_matricula", nullable = false)
    private LocalDate fechaMatricula;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private EstadoMatricula estado = EstadoMatricula.ACTIVA;

    @Column(name = "fecha_retiro")
    private LocalDate fechaRetiro;

    @Column(name = "motivo_retiro", length = 300)
    private String motivoRetiro;

    protected Matricula() {
    }

    public Matricula(Estudiante estudiante, AnioLectivo anioLectivo, LocalDate fechaMatricula) {
        this.estudiante = estudiante;
        this.anioLectivo = anioLectivo;
        this.fechaMatricula = fechaMatricula;
    }

    public boolean estaActiva() {
        return estado == EstadoMatricula.ACTIVA;
    }

    public void retirar(LocalDate fecha, String motivo) {
        estado = EstadoMatricula.RETIRADA;
        fechaRetiro = fecha;
        motivoRetiro = motivo;
    }

    public void reactivar(LocalDate fecha) {
        estado = EstadoMatricula.ACTIVA;
        fechaMatricula = fecha;
        fechaRetiro = null;
        motivoRetiro = null;
    }

    public Long getId() {
        return id;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public AnioLectivo getAnioLectivo() {
        return anioLectivo;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public void setGrupo(Grupo grupo) {
        this.grupo = grupo;
    }

    public LocalDate getFechaMatricula() {
        return fechaMatricula;
    }

    public EstadoMatricula getEstado() {
        return estado;
    }

    public LocalDate getFechaRetiro() {
        return fechaRetiro;
    }

    public String getMotivoRetiro() {
        return motivoRetiro;
    }
}
