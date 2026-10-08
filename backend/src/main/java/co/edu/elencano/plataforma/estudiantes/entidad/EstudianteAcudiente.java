package co.edu.elencano.plataforma.estudiantes.entidad;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

/** Vinculo entre un estudiante y uno de sus acudientes, con el parentesco. */
@Entity
@Table(name = "estudiante_acudiente")
public class EstudianteAcudiente {

    @EmbeddedId
    private Clave clave = new Clave();

    @MapsId("estudianteId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @MapsId("acudienteId")
    @ManyToOne(optional = false)
    @JoinColumn(name = "acudiente_id")
    private Acudiente acudiente;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Parentesco parentesco;

    @Column(nullable = false)
    private boolean principal;

    protected EstudianteAcudiente() {
    }

    EstudianteAcudiente(Estudiante estudiante, Acudiente acudiente, Parentesco parentesco) {
        this.estudiante = estudiante;
        this.acudiente = acudiente;
        this.parentesco = parentesco;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public Acudiente getAcudiente() {
        return acudiente;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public void setParentesco(Parentesco parentesco) {
        this.parentesco = parentesco;
    }

    public boolean isPrincipal() {
        return principal;
    }

    void setPrincipal(boolean principal) {
        this.principal = principal;
    }

    @Embeddable
    public static class Clave implements Serializable {

        @Column(name = "estudiante_id")
        private Long estudianteId;

        @Column(name = "acudiente_id")
        private Long acudienteId;

        @Override
        public boolean equals(Object otro) {
            return otro instanceof Clave c && Objects.equals(estudianteId, c.estudianteId)
                    && Objects.equals(acudienteId, c.acudienteId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(estudianteId, acudienteId);
        }
    }
}
