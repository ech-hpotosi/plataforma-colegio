package co.edu.elencano.plataforma.matricula.entidad;

import java.time.LocalDateTime;

import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/** Cambio de estado de una solicitud: quien lo hizo, cuando y con que comentario. */
@Entity
@Table(name = "revision_solicitud")
public class RevisionSolicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "solicitud_id", nullable = false)
    private SolicitudMatricula solicitud;

    @ManyToOne(optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false)
    private LocalDateTime fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_anterior", nullable = false, length = 20)
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(name = "estado_nuevo", nullable = false, length = 20)
    private EstadoSolicitud estadoNuevo;

    @Column(length = 500)
    private String comentario;

    protected RevisionSolicitud() {
    }

    RevisionSolicitud(SolicitudMatricula solicitud, Usuario usuario, LocalDateTime fecha,
                      EstadoSolicitud estadoAnterior, EstadoSolicitud estadoNuevo, String comentario) {
        this.solicitud = solicitud;
        this.usuario = usuario;
        this.fecha = fecha;
        this.estadoAnterior = estadoAnterior;
        this.estadoNuevo = estadoNuevo;
        this.comentario = comentario;
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public LocalDateTime getFecha() {
        return fecha;
    }

    public EstadoSolicitud getEstadoAnterior() {
        return estadoAnterior;
    }

    public EstadoSolicitud getEstadoNuevo() {
        return estadoNuevo;
    }

    public String getComentario() {
        return comentario;
    }
}
