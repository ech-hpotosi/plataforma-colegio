package co.edu.elencano.plataforma.academico.entidad;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Docente asignado a dictar una asignatura en un grupo. */
@Entity
@Table(name = "carga_academica")
public class CargaAcademica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grupo_id", nullable = false)
    private Grupo grupo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "asignatura_id", nullable = false)
    private Asignatura asignatura;

    @ManyToOne(optional = false)
    @JoinColumn(name = "docente_id", nullable = false)
    private Docente docente;

    protected CargaAcademica() {
    }

    public CargaAcademica(Grupo grupo, Asignatura asignatura, Docente docente) {
        this.grupo = grupo;
        this.asignatura = asignatura;
        this.docente = docente;
    }

    public Long getId() {
        return id;
    }

    public Grupo getGrupo() {
        return grupo;
    }

    public Asignatura getAsignatura() {
        return asignatura;
    }

    public Docente getDocente() {
        return docente;
    }

    public void setDocente(Docente docente) {
        this.docente = docente;
    }
}
