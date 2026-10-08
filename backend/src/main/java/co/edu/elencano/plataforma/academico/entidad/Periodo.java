package co.edu.elencano.plataforma.academico.entidad;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Periodo academico. El porcentaje es su peso en la nota final del anio. */
@Entity
@Table(name = "periodo")
public class Periodo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "anio_lectivo_id", nullable = false)
    private AnioLectivo anioLectivo;

    @Column(nullable = false)
    private int numero;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal porcentaje;

    @Column(nullable = false)
    private boolean cerrado;

    protected Periodo() {
    }

    public Periodo(int numero, LocalDate fechaInicio, LocalDate fechaFin, BigDecimal porcentaje) {
        this.numero = numero;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.porcentaje = porcentaje;
    }

    void asignarAnio(AnioLectivo anioLectivo) {
        this.anioLectivo = anioLectivo;
    }

    void actualizar(LocalDate fechaInicio, LocalDate fechaFin, BigDecimal porcentaje) {
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
        this.porcentaje = porcentaje;
    }

    public Long getId() {
        return id;
    }

    public AnioLectivo getAnioLectivo() {
        return anioLectivo;
    }

    public int getNumero() {
        return numero;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public BigDecimal getPorcentaje() {
        return porcentaje;
    }

    public boolean isCerrado() {
        return cerrado;
    }
}
