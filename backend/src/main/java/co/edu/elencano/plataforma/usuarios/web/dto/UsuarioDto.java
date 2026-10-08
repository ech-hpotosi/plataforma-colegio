package co.edu.elencano.plataforma.usuarios.web.dto;

import java.time.LocalDateTime;
import java.util.Set;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;

/** Usuario con sus datos personales, para la administracion de usuarios. Nunca incluye la contrasena. */
public record UsuarioDto(
        Long id,
        String nombreUsuario,
        boolean activo,
        boolean bloqueado,
        LocalDateTime ultimoAcceso,
        Set<Rol> roles,
        TipoDocumento tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String telefono,
        String correo) {

    public static UsuarioDto de(Usuario usuario) {
        Persona persona = usuario.getPersona();
        return new UsuarioDto(
                usuario.getId(),
                usuario.getNombreUsuario(),
                usuario.isActivo(),
                usuario.estaBloqueado(LocalDateTime.now()),
                usuario.getUltimoAcceso(),
                Set.copyOf(usuario.getRoles()),
                persona.getTipoDocumento(),
                persona.getNumeroDocumento(),
                persona.getNombres(),
                persona.getApellidos(),
                persona.getTelefono(),
                persona.getCorreo());
    }
}
