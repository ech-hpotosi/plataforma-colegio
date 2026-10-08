package co.edu.elencano.plataforma.academico.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Asignatura que se dicta en un grado durante un anio lectivo, con su intensidad horaria semanal. */
@Entity
@Table(name = "plan_estudio")
public class PlanEstudio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anio_lectivo_id", nullable = false)
    private AnioLectivo anioLectivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @Column(name = "intensidad_horaria", nullable = false)
    private int intensidadHoraria;

    protected PlanEstudio() {
    }

    public PlanEstudio(AnioLectivo anioLectivo, Grado grado, Asignatura asignatura, int intensidadHoraria) {
        this.anioLectivo = anioLectivo;
        this.grado = grado;
        this.asignatura = asignatura;
        this.intensidadHoraria = intensidadHoraria;
    }

    public Long getId() {
        return id;
    }

    public AnioLectivo getAnioLectivo() {
        return anioLectivo;
    }

    public Grado getGrado() {
        return grado;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public int getIntensidadHoraria() {
        return intensidadHoraria;
    }

    public void setIntensidadHoraria(int intensidadHoraria) {
        this.intensidadHoraria = intensidadHoraria;
    }
}
