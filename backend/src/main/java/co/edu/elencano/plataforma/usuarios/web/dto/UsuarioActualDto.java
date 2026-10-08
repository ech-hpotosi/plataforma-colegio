package co.edu.elencano.plataforma.usuarios.web.dto;

import java.util.Set;

import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;

/** Usuario que tiene la sesion abierta. Lo usa el frontend para mostrar el menu segun los roles. */
public record UsuarioActualDto(Long id, String nombreUsuario, String nombreCompleto, Set<Rol> roles) {

    public static UsuarioActualDto de(UsuarioAutenticado usuario) {
        return new UsuarioActualDto(usuario.getId(), usuario.getUsername(), usuario.getNombreCompleto(),
                usuario.getRoles());
    }
}
