package co.edu.elencano.plataforma.matricula.entidad;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;

/**
 * Solicitud de matricula en linea que diligencia un acudiente. Guarda los datos del estudiante tal como
 * los escribio el acudiente; solo pasan a la ficha del estudiante cuando la secretaria la aprueba.
 */
@Entity
@Table(name = "solicitud_matricula")
public class SolicitudMatricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "anio_lectivo_id", nullable = false)
    private AnioLectivo anioLectivo;

    @ManyToOne(optional = false)
    @JoinColumn(name = "sede_id", nullable = false)
    private Sede sede;

    @ManyToOne(optional = false)
    @JoinColumn(name = "grado_id", nullable = false)
    private Grado grado;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitante_id", nullable = false)
    private Usuario solicitante;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 15)
    private Parentesco parentesco;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoSolicitud tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoSolicitud estado = EstadoSolicitud.BORRADOR;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 5)
    private TipoDocumento tipoDocumento;

    @Column(name = "numero_documento", nullable = false, length = 20)
    private String numeroDocumento;

    @Column(nullable = false, length = 100)
    private String nombres;

    @Column(nullable = false, length = 100)
    private String apellidos;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Genero genero;

    @Column(length = 200)
    private String direccion;

    @Column(length = 20)
    private String telefono;

    @Column(length = 150)
    private String correo;

    @Column(length = 100)
    private String eps;

    @Column(name = "grupo_sanguineo", length = 5)
    private String grupoSanguineo;

    @Column(name = "condicion_discapacidad", length = 150)
    private String condicionDiscapacidad;

    @Column(name = "condiciones_especiales", length = 500)
    private String condicionesEspeciales;

    @Column(name = "institucion_anterior", length = 150)
    private String institucionAnterior;

    @Column(name = "autoriza_datos", nullable = false)
    private boolean autorizaDatos;

    @Column(name = "fecha_autorizacion")
    private LocalDateTime fechaAutorizacion;

    @Column(name = "fecha_creacion", nullable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_envio")
    private LocalDateTime fechaEnvio;

    /** Ultimo comentario de la secretaria al devolverla o rechazarla. */
    @Column(length = 500)
    private String observacion;

    @ManyToOne
    @JoinColumn(name = "estudiante_id")
    private Estudiante estudiante;

    @OneToOne
    @JoinColumn(name = "matricula_id")
    private Matricula matricula;

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DocumentoAdjunto> documentos = new ArrayList<>();

    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("fecha")
    private List<RevisionSolicitud> revisiones = new ArrayList<>();

    protected SolicitudMatricula() {
    }

    public SolicitudMatricula(AnioLectivo anioLectivo, Usuario solicitante, LocalDateTime fechaCreacion) {
        this.anioLectivo = anioLectivo;
        this.solicitante = solicitante;
        this.fechaCreacion = fechaCreacion;
    }

    public DocumentoAdjunto documentoDe(Long tipoDocumentoId) {
        return documentos.stream()
                .filter(d -> d.getTipoDocumento().getId().equals(tipoDocumentoId))
                .findFirst().orElse(null);
    }

    /** Devuelve el documento de ese tipo, creandolo si todavia no existe. */
    public DocumentoAdjunto documentoPara(TipoDocumentoRequerido tipo) {
        DocumentoAdjunto documento = documentoDe(tipo.getId());
        if (documento == null) {
            documento = new DocumentoAdjunto(this, tipo);
            documentos.add(documento);
        }
        return documento;
    }

    public void quitarDocumento(DocumentoAdjunto documento) {
        documentos.remove(documento);
    }

    /** Cambia el estado y deja el registro en el historial. */
    public void cambiarEstado(EstadoSolicitud nuevo, Usuario usuario, LocalDateTime fecha, String comentario) {
        revisiones.add(new RevisionSolicitud(this, usuario, fecha, estado, nuevo, comentario));
        if (nuevo == EstadoSolicitud.ENVIADA) {
            fechaEnvio = fecha;
        }
        if (nuevo == EstadoSolicitud.CON_OBSERVACIONES || nuevo == EstadoSolicitud.RECHAZADA) {
            observacion = comentario;
        }
        estado = nuevo;
    }

    public void autorizarDatos(LocalDateTime fecha) {
        if (!autorizaDatos) {
            autorizaDatos = true;
            fechaAutorizacion = fecha;
        }
    }

    public void registrarAprobacion(Estudiante estudiante, Matricula matricula) {
        this.estudiante = estudiante;
        this.matricula = matricula;
    }

    public Long getId() {
        return id;
    }

    public AnioLectivo getAnioLectivo() {
        return anioLectivo;
    }

    public Sede getSede() {
        return sede;
    }

    public void setSede(Sede sede) {
        this.sede = sede;
    }

    public Grado getGrado() {
        return grado;
    }

    public void setGrado(Grado grado) {
        this.grado = grado;
    }

    public Usuario getSolicitante() {
        return solicitante;
    }

    public Parentesco getParentesco() {
        return parentesco;
    }

    public void setParentesco(Parentesco parentesco) {
        this.parentesco = parentesco;
    }

    public TipoSolicitud getTipo() {
        return tipo;
    }

    public void setTipo(TipoSolicitud tipo) {
        this.tipo = tipo;
    }

    public EstadoSolicitud getEstado() {
        return estado;
    }

    public TipoDocumento getTipoDocumento() {
        return tipoDocumento;
    }

    public void setTipoDocumento(TipoDocumento tipoDocumento) {
        this.tipoDocumento = tipoDocumento;
    }

    public String getNumeroDocumento() {
        return numeroDocumento;
    }

    public void setNumeroDocumento(String numeroDocumento) {
        this.numeroDocumento = numeroDocumento;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
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

    public String getTelefono() {
        return telefono;
    }

    public void setTelefono(String telefono) {
        this.telefono = telefono;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
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

    public String getCondicionesEspeciales() {
        return condicionesEspeciales;
    }

    public void setCondicionesEspeciales(String condicionesEspeciales) {
        this.condicionesEspeciales = condicionesEspeciales;
    }

    public String getInstitucionAnterior() {
        return institucionAnterior;
    }

    public void setInstitucionAnterior(String institucionAnterior) {
        this.institucionAnterior = institucionAnterior;
    }

    public boolean isAutorizaDatos() {
        return autorizaDatos;
    }

    public LocalDateTime getFechaAutorizacion() {
        return fechaAutorizacion;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaEnvio() {
        return fechaEnvio;
    }

    public String getObservacion() {
        return observacion;
    }

    public Estudiante getEstudiante() {
        return estudiante;
    }

    public Matricula getMatricula() {
        return matricula;
    }

    public List<DocumentoAdjunto> getDocumentos() {
        return documentos;
    }

    public List<RevisionSolicitud> getRevisiones() {
        return revisiones;
    }
}
