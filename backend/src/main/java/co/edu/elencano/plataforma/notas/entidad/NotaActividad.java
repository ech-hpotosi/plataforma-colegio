package co.edu.elencano.plataforma.notas.entidad;

import java.math.BigDecimal;

import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Nota de un estudiante (por su matricula) en una actividad. Si no hay fila, la actividad no tiene nota. */
@Entity
@Table(name = "nota_actividad")
public class NotaActividad {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "actividad_id", nullable = false)
    private ActividadEvaluativa actividad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal valor;

    protected NotaActividad() {
    }

    public NotaActividad(ActividadEvaluativa actividad, Matricula matricula, BigDecimal valor) {
        this.actividad = actividad;
        this.matricula = matricula;
        this.valor = valor;
    }

    public Long getId() {
        return id;
    }

    public ActividadEvaluativa getActividad() {
        return actividad;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    /** Id de la matricula sin cargarla. */
    public Long getMatriculaId() {
        return matricula.getId();
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }
}
