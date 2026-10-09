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
 * Nota de recuperacion (superacion) de un estudiante en una asignatura: de un periodo,
 * o final del anio cuando periodo es nulo (SIEE art. 9 y 11).
 */
@Entity
@Table(name = "recuperacion")
public class Recuperacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_academica_id", nullable = false)
    private CargaAcademica carga;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "periodo_id")
    private Periodo periodo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "matricula_id", nullable = false)
    private Matricula matricula;

    @Column(nullable = false, precision = 2, scale = 1)
    private BigDecimal nota;

    @Column(length = 300)
    private String observacion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    protected Recuperacion() {
    }

    public Recuperacion(CargaAcademica carga, Periodo periodo, Matricula matricula) {
        this.carga = carga;
        this.periodo = periodo;
        this.matricula = matricula;
    }

    public void registrar(BigDecimal nota, String observacion, Usuario usuario, LocalDateTime ahora) {
        this.nota = nota;
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

    /** Id del periodo, o nulo si es la recuperacion final. */
    public Long getPeriodoId() {
        return periodo == null ? null : periodo.getId();
    }

    public Long getMatriculaId() {
        return matricula.getId();
    }

    public BigDecimal getNota() {
        return nota;
    }

    public String getObservacion() {
        return observacion;
    }
}
