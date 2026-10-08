package co.edu.elencano.plataforma.academico.entidad;

import java.util.HashSet;
import java.util.Set;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Datos academicos de una persona que es docente. Comparte la clave con persona.
 * Las sedes indican donde trabaja; si no tiene sedes se puede asignar en cualquiera.
 */
@Entity
@Table(name = "docente")
public class Docente {

    @Id
    @Column(name = "persona_id")
    private Long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Column(length = 150)
    private String especialidad;

    @Column(length = 30)
    private String escalafon;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "docente_sede",
            joinColumns = @JoinColumn(name = "docente_id"),
            inverseJoinColumns = @JoinColumn(name = "sede_id"))
    private Set<Sede> sedes = new HashSet<>();

    protected Docente() {
    }

    public Docente(Persona persona) {
        this.persona = persona;
    }

    /** Indica si el docente puede trabajar en la sede dada. */
    public boolean trabajaEn(Sede sede) {
        return sedes.isEmpty() || sedes.stream().anyMatch(s -> s.getId().equals(sede.getId()));
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public String getEspecialidad() {
        return especialidad;
    }

    public void setEspecialidad(String especialidad) {
        this.especialidad = especialidad;
    }

    public String getEscalafon() {
        return escalafon;
    }

    public void setEscalafon(String escalafon) {
        this.escalafon = escalafon;
    }

    public Set<Sede> getSedes() {
        return sedes;
    }

    public void setSedes(Set<Sede> nuevas) {
        sedes.clear();
        sedes.addAll(nuevas);
    }
}
