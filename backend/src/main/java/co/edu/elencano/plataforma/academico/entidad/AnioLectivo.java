package co.edu.elencano.plataforma.academico.entidad;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/** Anio lectivo con sus periodos academicos. */
@Entity
@Table(name = "anio_lectivo")
public class AnioLectivo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private int anio;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDate fechaInicio;

    @Column(name = "fecha_fin", nullable = false)
    private LocalDate fechaFin;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private EstadoAnio estado = EstadoAnio.PLANEACION;

    @OneToMany(mappedBy = "anioLectivo", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
    @OrderBy("numero")
    private List<Periodo> periodos = new ArrayList<>();

    protected AnioLectivo() {
    }

    public AnioLectivo(int anio, LocalDate fechaInicio, LocalDate fechaFin) {
        this.anio = anio;
        this.fechaInicio = fechaInicio;
        this.fechaFin = fechaFin;
    }

    public boolean estaCerrado() {
        return estado == EstadoAnio.CERRADO;
    }

    /** Reemplaza los periodos. Los existentes se actualizan para no perder su identificador. */
    public void reemplazarPeriodos(List<Periodo> nuevos) {
        while (periodos.size() > nuevos.size()) {
            periodos.removeLast();
        }
        for (int i = 0; i < nuevos.size(); i++) {
            Periodo nuevo = nuevos.get(i);
            if (i < periodos.size()) {
                periodos.get(i).actualizar(nuevo.getFechaInicio(), nuevo.getFechaFin(), nuevo.getPorcentaje());
            } else {
                nuevo.asignarAnio(this);
                periodos.add(nuevo);
            }
        }
    }

    public Long getId() {
        return id;
    }

    public int getAnio() {
        return anio;
    }

    public LocalDate getFechaInicio() {
        return fechaInicio;
    }

    public void setFechaInicio(LocalDate fechaInicio) {
        this.fechaInicio = fechaInicio;
    }

    public LocalDate getFechaFin() {
        return fechaFin;
    }

    public void setFechaFin(LocalDate fechaFin) {
        this.fechaFin = fechaFin;
    }

    public EstadoAnio getEstado() {
        return estado;
    }

    public void setEstado(EstadoAnio estado) {
        this.estado = estado;
    }

    public List<Periodo> getPeriodos() {
        return periodos;
    }
}
