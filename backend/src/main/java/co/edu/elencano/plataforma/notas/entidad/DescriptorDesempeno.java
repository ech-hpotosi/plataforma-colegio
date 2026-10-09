package co.edu.elencano.plataforma.notas.entidad;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;
import co.edu.elencano.plataforma.academico.entidad.Periodo;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Concepto descriptivo de un desempeno en una clase y periodo (SIEE art. 14). El boletin muestra a cada
 * estudiante el texto del desempeno que obtuvo.
 */
@Entity
@Table(name = "descriptor_desempeno")
public class DescriptorDesempeno {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "carga_academica_id", nullable = false)
    private CargaAcademica carga;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "periodo_id", nullable = false)
    private Periodo periodo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Desempeno desempeno;

    @Column(nullable = false, length = 600)
    private String descripcion;

    protected DescriptorDesempeno() {
    }

    public DescriptorDesempeno(CargaAcademica carga, Periodo periodo, Desempeno desempeno) {
        this.carga = carga;
        this.periodo = periodo;
        this.desempeno = desempeno;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Long getId() {
        return id;
    }

    /** Id de la carga sin cargarla. */
    public Long getCargaId() {
        return carga.getId();
    }

    public Desempeno getDesempeno() {
        return desempeno;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
