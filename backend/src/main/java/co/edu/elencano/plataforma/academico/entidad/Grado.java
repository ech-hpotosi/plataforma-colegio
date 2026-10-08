package co.edu.elencano.plataforma.academico.entidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Grado escolar (Transicion a Undecimo). Se cargan en la migracion y no se crean desde la aplicacion. */
@Entity
@Table(name = "grado")
public class Grado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 40)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Nivel nivel;

    @Column(nullable = false)
    private int orden;

    @Column(name = "evaluacion_cualitativa", nullable = false)
    private boolean evaluacionCualitativa;

    protected Grado() {
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public Nivel getNivel() {
        return nivel;
    }

    public int getOrden() {
        return orden;
    }

    public boolean isEvaluacionCualitativa() {
        return evaluacionCualitativa;
    }

    public void setEvaluacionCualitativa(boolean evaluacionCualitativa) {
        this.evaluacionCualitativa = evaluacionCualitativa;
    }
}
