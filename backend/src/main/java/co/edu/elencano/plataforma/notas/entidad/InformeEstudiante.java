package co.edu.elencano.plataforma.notas.entidad;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Lo que el docente registra de un estudiante en su clase para el boletin del periodo (SIEE art. 14):
 * valoracion del comportamiento y observacion. El boletin promedia el comportamiento de todos los docentes.
 */
@Entity
@Table(name = "informe_estudiante")
public class InformeEstudiante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_academica_id", nullable = false)
    private CargaAcademica carga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Column(precision = 2, scale = 1)
    private BigDecimal comportamiento;

    @Column(length = 500)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected InformeEstudiante() {
    }

    public InformeEstudiante(CargaAcademica carga, Periodo periodo, Matricula matricula) {
        this.carga = carga;
        this.periodo = periodo;
        this.matricula = matricula;
    }

    public void registrar(BigDecimal comportamiento, String observacion, Usuario usuario, LocalDateTime ahora) {
        this.comportamiento = comportamiento;
        this.observacion = observacion;
        this.registradoPor = usuario;
        this.fechaRegistro = ahora;
    }

    public Long getId() {
        return id;
    }

    /** Id de la carga sin cargarla. */
    public Long getCargaId() {
        return carga.getId();
    }

    /** Id de la matricula sin cargarla. */
    public Long getMatriculaId() {
        return matricula.getId();
    }

    public BigDecimal getComportamiento() {
        return comportamiento;
    }

    public String getObservacion() {
        return observacion;
    }
}
