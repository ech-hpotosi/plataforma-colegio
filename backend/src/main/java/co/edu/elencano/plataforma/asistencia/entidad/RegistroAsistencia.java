package co.edu.elencano.plataforma.asistencia.entidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

/** Asistencia tomada en una clase: una carga academica (grupo y asignatura) en una fecha. */
@Entity
@Table(name = "registro_asistencia")
public class RegistroAsistencia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "carga_academica_id", nullable = false)
    private CargaAcademica carga;

    @ManyToOne(optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private int horas;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registrado_por", nullable = false)
    private Usuario registradoPor;

    @Column(name = "fecha_registro", nullable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "registro", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DetalleAsistencia> detalles = new ArrayList<>();

    protected RegistroAsistencia() {
    }

    public RegistroAsistencia(CargaAcademica carga, Periodo periodo, LocalDate fecha) {
        this.carga = carga;
        this.periodo = periodo;
        this.fecha = fecha;
    }

    /** Guarda quien tomo la asistencia y cuando; si se corrige despues queda el ultimo. */
    public void registrar(int horas, Usuario usuario, LocalDateTime ahora) {
        this.horas = horas;
        this.registradoPor = usuario;
        this.fechaRegistro = ahora;
    }

    public Optional<DetalleAsistencia> detalleDe(Long matriculaId) {
        return detalles.stream().filter(d -> d.getMatricula().getId().equals(matriculaId)).findFirst();
    }

    public DetalleAsistencia agregarDetalle(Matricula matricula) {
        DetalleAsistencia detalle = new DetalleAsistencia(this, matricula);
        detalles.add(detalle);
        return detalle;
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

    public LocalDate getFecha() {
        return fecha;
    }

    public int getHoras() {
        return horas;
    }

    public Usuario getRegistradoPor() {
        return registradoPor;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public List<DetalleAsistencia> getDetalles() {
        return detalles;
    }
}
