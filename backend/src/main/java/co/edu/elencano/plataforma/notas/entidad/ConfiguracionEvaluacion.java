package co.edu.elencano.plataforma.notas.entidad;

import java.math.BigDecimal;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Escala numerica y pesos de las dimensiones para un anio lectivo. El SIEE no fija numeros,
 * por eso los valores por defecto son provisionales y los cambia el administrador o coordinacion.
 * Rangos: Bajo menor que la nota aprobatoria; Basico hasta antes de limiteAlto; Alto hasta antes de
 * limiteSuperior; Superior desde limiteSuperior.
 */
@Entity
@Table(name = "configuracion_evaluacion")
public class ConfiguracionEvaluacion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "anio_lectivo_id", nullable = false, unique = true)
    private AnioLectivo anioLectivo;

    @Column(name = "nota_minima", nullable = false, precision = 2, scale = 1)
    private BigDecimal notaMinima;

    @Column(name = "nota_maxima", nullable = false, precision = 2, scale = 1)
    private BigDecimal notaMaxima;

    @Column(name = "nota_aprobatoria", nullable = false, precision = 2, scale = 1)
    private BigDecimal notaAprobatoria;

    @Column(name = "limite_alto", nullable = false, precision = 2, scale = 1)
    private BigDecimal limiteAlto;

    @Column(name = "limite_superior", nullable = false, precision = 2, scale = 1)
    private BigDecimal limiteSuperior;

    /** Nota maxima que puede quedar despues de una recuperacion; por defecto la nota aprobatoria. */
    @Column(name = "tope_recuperacion", nullable = false, precision = 2, scale = 1)
    private BigDecimal topeRecuperacion;

    @Column(name = "peso_saber", nullable = false)
    private int pesoSaber;

    @Column(name = "peso_hacer", nullable = false)
    private int pesoHacer;

    @Column(name = "peso_ser", nullable = false)
    private int pesoSer;

    protected ConfiguracionEvaluacion() {
    }

    /**
     * Valores por defecto: escala 1.0 a 5.0, aprueba con 3.0, Alto desde 4.0, Superior desde 4.6, pesos 40/40/20
     * y la recuperacion deja como maximo 3.0.
     */
    public static ConfiguracionEvaluacion porDefecto(AnioLectivo anio) {
        ConfiguracionEvaluacion c = new ConfiguracionEvaluacion();
        c.anioLectivo = anio;
        c.actualizar(new BigDecimal("1.0"), new BigDecimal("5.0"), new BigDecimal("3.0"), new BigDecimal("4.0"),
                new BigDecimal("4.6"), new BigDecimal("3.0"), 40, 40, 20);
        return c;
    }

    public void actualizar(BigDecimal notaMinima, BigDecimal notaMaxima, BigDecimal notaAprobatoria,
                           BigDecimal limiteAlto, BigDecimal limiteSuperior, BigDecimal topeRecuperacion,
                           int pesoSaber, int pesoHacer, int pesoSer) {
        this.topeRecuperacion = topeRecuperacion;
        this.notaMinima = notaMinima;
        this.notaMaxima = notaMaxima;
        this.notaAprobatoria = notaAprobatoria;
        this.limiteAlto = limiteAlto;
        this.limiteSuperior = limiteSuperior;
        this.pesoSaber = pesoSaber;
        this.pesoHacer = pesoHacer;
        this.pesoSer = pesoSer;
    }

    public int pesoDe(Dimension dimension) {
        return switch (dimension) {
            case SABER -> pesoSaber;
            case HACER -> pesoHacer;
            case SER -> pesoSer;
        };
    }

    public Desempeno desempenoDe(BigDecimal nota) {
        if (nota.compareTo(limiteSuperior) >= 0) {
            return Desempeno.SUPERIOR;
        }
        if (nota.compareTo(limiteAlto) >= 0) {
            return Desempeno.ALTO;
        }
        if (nota.compareTo(notaAprobatoria) >= 0) {
            return Desempeno.BASICO;
        }
        return Desempeno.BAJO;
    }

    public boolean estaEnEscala(BigDecimal valor) {
        return valor.compareTo(notaMinima) >= 0 && valor.compareTo(notaMaxima) <= 0;
    }

    public Long getId() {
        return id;
    }

    public AnioLectivo getAnioLectivo() {
        return anioLectivo;
    }

    public BigDecimal getNotaMinima() {
        return notaMinima;
    }

    public BigDecimal getNotaMaxima() {
        return notaMaxima;
    }

    public BigDecimal getNotaAprobatoria() {
        return notaAprobatoria;
    }

    public BigDecimal getLimiteAlto() {
        return limiteAlto;
    }

    public BigDecimal getLimiteSuperior() {
        return limiteSuperior;
    }

    public BigDecimal getTopeRecuperacion() {
        return topeRecuperacion;
    }

    public int getPesoSaber() {
        return pesoSaber;
    }

    public int getPesoHacer() {
        return pesoHacer;
    }

    public int getPesoSer() {
        return pesoSer;
    }
}
