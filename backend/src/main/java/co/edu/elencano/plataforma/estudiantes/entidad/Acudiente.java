package co.edu.elencano.plataforma.estudiantes.entidad;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Padre, madre o tutor. Comparte la clave con persona; puede tener varios estudiantes a cargo. */
@Entity
@Table(name = "acudiente")
public class Acudiente {

    @Id
    @Column(name = "persona_id")
    private Long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Column(length = 100)
    private String ocupacion;

    protected Acudiente() {
    }

    public Acudiente(Persona persona) {
        this.persona = persona;
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public String getOcupacion() {
        return ocupacion;
    }

    public void setOcupacion(String ocupacion) {
        this.ocupacion = ocupacion;
    }
}
