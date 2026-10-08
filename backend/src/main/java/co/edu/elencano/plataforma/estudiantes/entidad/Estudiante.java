package co.edu.elencano.plataforma.estudiantes.entidad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Ficha del estudiante. Comparte la clave con persona, donde estan el documento y los nombres. */
@Entity
@Table(name = "estudiante")
public class Estudiante {

    @Id
    @Column(name = "persona_id")
    private Long id;

    @MapsId
    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id")
    private Persona persona;

    @Column(length = 20)
    private String codigo;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Genero genero;

    @Column(length = 200)
    private String direccion;

    @Column(length = 100)
    private String eps;

    @Column(name = "grupo_sanguineo", length = 5)
    private String grupoSanguineo;

    @Column(name = "condicion_discapacidad", length = 150)
    private String condicionDiscapacidad;

    @Column(name = "tiene_piar", nullable = false)
    private boolean tienePiar;

    @Column(name = "condiciones_especiales", length = 500)
    private String condicionesEspeciales;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoEstudiante estado = EstadoEstudiante.ASPIRANTE;

    @OneToMany(mappedBy = "estudiante", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<EstudianteAcudiente> acudientes = new ArrayList<>();

    protected Estudiante() {
    }

    public Estudiante(Persona persona) {
        this.persona = persona;
    }

    /** Busca el vinculo con un acudiente, o null si no esta vinculado. */
    public EstudianteAcudiente vinculoCon(Long acudienteId) {
        return acudientes.stream().filter(v -> v.getAcudiente().getId().equals(acudienteId)).findFirst().orElse(null);
    }

    public EstudianteAcudiente vincular(Acudiente acudiente, Parentesco parentesco) {
        EstudianteAcudiente vinculo = new EstudianteAcudiente(this, acudiente, parentesco);
        acudientes.add(vinculo);
        return vinculo;
    }

    public void desvincular(EstudianteAcudiente vinculo) {
        acudientes.remove(vinculo);
    }

    /** Deja como principal solo al acudiente indicado. */
    public void marcarPrincipal(EstudianteAcudiente principal) {
        acudientes.forEach(v -> v.setPrincipal(v == principal));
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public String getCodigo() {
        return codigo;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public LocalDate getFechaNacimiento() {
        return fechaNacimiento;
    }

    public void setFechaNacimiento(LocalDate fechaNacimiento) {
        this.fechaNacimiento = fechaNacimiento;
    }

    public Genero getGenero() {
        return genero;
    }

    public void setGenero(Genero genero) {
        this.genero = genero;
    }

    public String getDireccion() {
        return direccion;
    }

    public void setDireccion(String direccion) {
        this.direccion = direccion;
    }

    public String getEps() {
        return eps;
    }

    public void setEps(String eps) {
        this.eps = eps;
    }

    public String getGrupoSanguineo() {
        return grupoSanguineo;
    }

    public void setGrupoSanguineo(String grupoSanguineo) {
        this.grupoSanguineo = grupoSanguineo;
    }

    public String getCondicionDiscapacidad() {
        return condicionDiscapacidad;
    }

    public void setCondicionDiscapacidad(String condicionDiscapacidad) {
        this.condicionDiscapacidad = condicionDiscapacidad;
    }

    public boolean isTienePiar() {
        return tienePiar;
    }

    public void setTienePiar(boolean tienePiar) {
        this.tienePiar = tienePiar;
    }

    public String getCondicionesEspeciales() {
        return condicionesEspeciales;
    }

    public void setCondicionesEspeciales(String condicionesEspeciales) {
        this.condicionesEspeciales = condicionesEspeciales;
    }

    public EstadoEstudiante getEstado() {
        return estado;
    }

    public void setEstado(EstadoEstudiante estado) {
        this.estado = estado;
    }

    public List<EstudianteAcudiente> getAcudientes() {
        return acudientes;
    }
}
