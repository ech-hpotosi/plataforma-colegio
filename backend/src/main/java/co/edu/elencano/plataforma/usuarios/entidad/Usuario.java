package co.edu.elencano.plataforma.usuarios.entidad;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Set;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/** Cuenta de acceso a la plataforma. Un usuario puede tener varios roles. */
@Entity
@Table(name = "usuario")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(optional = false)
    @JoinColumn(name = "persona_id", nullable = false, unique = true)
    private Persona persona;

    @Column(name = "nombre_usuario", nullable = false, unique = true, length = 50)
    private String nombreUsuario;

    @Column(name = "contrasena_hash", nullable = false, length = 100)
    private String contrasenaHash;

    @Column(nullable = false)
    private boolean activo = true;

    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    @Column(name = "ultimo_acceso")
    private LocalDateTime ultimoAcceso;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "usuario_rol", joinColumns = @JoinColumn(name = "usuario_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rol", nullable = false, length = 30)
    private Set<Rol> roles = EnumSet.noneOf(Rol.class);

    protected Usuario() {
    }

    public Usuario(Persona persona, String nombreUsuario, String contrasenaHash) {
        this.persona = persona;
        this.nombreUsuario = nombreUsuario;
        this.contrasenaHash = contrasenaHash;
    }

    public boolean tieneRol(Rol rol) {
        return roles.contains(rol);
    }

    public void agregarRol(Rol rol) {
        roles.add(rol);
    }

    public boolean estaBloqueado(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /**
     * Suma un intento fallido. Al llegar al maximo, bloquea la cuenta hasta la fecha indicada
     * y reinicia el contador para el siguiente ciclo.
     */
    public void registrarIntentoFallido(int maximoIntentos, LocalDateTime bloquearHasta) {
        intentosFallidos++;
        if (intentosFallidos >= maximoIntentos) {
            bloqueadoHasta = bloquearHasta;
            intentosFallidos = 0;
        }
    }

    public void registrarAccesoExitoso(LocalDateTime ahora) {
        intentosFallidos = 0;
        bloqueadoHasta = null;
        ultimoAcceso = ahora;
    }

    public void desbloquear() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

    public Long getId() {
        return id;
    }

    public Persona getPersona() {
        return persona;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public String getContrasenaHash() {
        return contrasenaHash;
    }

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void setContrasenaHash(String contrasenaHash) {
        this.contrasenaHash = contrasenaHash;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public LocalDateTime getUltimoAcceso() {
        return ultimoAcceso;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    public void setRoles(Set<Rol> nuevosRoles) {
        roles.clear();
        roles.addAll(nuevosRoles);
    }
}
