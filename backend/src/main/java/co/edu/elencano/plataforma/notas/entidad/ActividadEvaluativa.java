package co.edu.elencano.plataforma.notas.entidad;

import java.time.LocalDate;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
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

/** Actividad que el docente califica (taller, evaluacion, exposicion) en una dimension del periodo. */
@Entity
@Table(name = "actividad_evaluativa")
public class ActividadEvaluativa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carga_academica_id", nullable = false)
    private CargaAcademica carga;

    @ManyToOne(optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Dimension dimension;

    @Column(nullable = false, length = 120)
    private String nombre;

    private LocalDate fecha;

    protected ActividadEvaluativa() {
    }

    public ActividadEvaluativa(CargaAcademica carga, Periodo periodo) {
        this.carga = carga;
        this.periodo = periodo;
    }

    public void actualizar(Dimension dimension, String nombre, LocalDate fecha) {
        this.dimension = dimension;
        this.nombre = nombre;
        this.fecha = fecha;
    }

    public Long getId() {
        return id;
    }

    public CargaAcademica getCarga() {
        return carga;
    }

    public Periodo getPeriodo() {
        return periodo;
    }

    public Dimension getDimension() {
        return dimension;
    }

    public String getNombre() {
        return nombre;
    }

    public LocalDate getFecha() {
        return fecha;
    }
}
