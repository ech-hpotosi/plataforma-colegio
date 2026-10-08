package co.edu.elencano.plataforma.usuarios.seguridad;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;

/**
 * Datos del usuario que Spring Security guarda en la sesion.
 * Cada rol se publica como autoridad ROLE_NOMBRE, que es lo que espera hasRole("NOMBRE").
 */
public class UsuarioAutenticado implements UserDetails {

    private final Long id;
    private final String nombreUsuario;
    private final String contrasenaHash;
    private final String nombreCompleto;
    private final Set<Rol> roles;
    private final boolean activo;
    private final boolean bloqueado;

    public UsuarioAutenticado(Usuario usuario) {
        this.id = usuario.getId();
        this.nombreUsuario = usuario.getNombreUsuario();
        this.contrasenaHash = usuario.getContrasenaHash();
        this.nombreCompleto = usuario.getPersona().getNombreCompleto();
        this.roles = usuario.getRoles().isEmpty() ? EnumSet.noneOf(Rol.class) : EnumSet.copyOf(usuario.getRoles());
        this.activo = usuario.isActivo();
        this.bloqueado = usuario.estaBloqueado(LocalDateTime.now());
    }

    public Long getId() {
        return id;
    }

    public String getNombreCompleto() {
        return nombreCompleto;
    }

    public Set<Rol> getRoles() {
        return roles;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(rol -> new SimpleGrantedAuthority("ROLE_" + rol.name())).toList();
    }

    @Override
    public String getPassword() {
        return contrasenaHash;
    }

    @Override
    public String getUsername() {
        return nombreUsuario;
    }

    @Override
    public boolean isAccountNonLocked() {
        return !bloqueado;
    }

    @Override
    public boolean isEnabled() {
        return activo;
    }
}
