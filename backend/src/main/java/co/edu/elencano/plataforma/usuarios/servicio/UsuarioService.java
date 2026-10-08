package co.edu.elencano.plataforma.usuarios.servicio;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.PersonaRepository;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;
import co.edu.elencano.plataforma.usuarios.web.dto.ActualizarUsuarioDto;
import co.edu.elencano.plataforma.usuarios.web.dto.CrearUsuarioDto;

/** Administracion de usuarios: crear, editar, activar o desactivar, cambiar contrasena y desbloquear. */
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository usuarioRepository,
                          PersonaRepository personaRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public Page<Usuario> buscar(String texto, int pagina, int tamano) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, 100),
                Sort.by("persona.apellidos", "persona.nombres"));
        Page<Usuario> resultado = usuarioRepository.buscar(filtro, paginacion);
        // Carga los roles dentro de la transaccion para poder convertirlos a DTO despues
        resultado.forEach(usuario -> usuario.getRoles().size());
        return resultado;
    }

    @Transactional(readOnly = true)
    public Usuario obtener(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el usuario " + id));
    }

    /**
     * Crea el usuario. Si la persona ya existe (mismo tipo y numero de documento) se reutiliza,
     * siempre que no tenga ya un usuario.
     */
    @Transactional
    public Usuario crear(CrearUsuarioDto datos) {
        if (usuarioRepository.existsByNombreUsuario(datos.nombreUsuario())) {
            throw new ReglaNegocioException("El nombre de usuario " + datos.nombreUsuario() + " ya esta en uso");
        }
        Persona persona = personaRepository
                .findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), datos.numeroDocumento())
                .orElseGet(() -> new Persona(datos.tipoDocumento(), datos.numeroDocumento(),
                        datos.nombres(), datos.apellidos()));
        if (persona.getId() != null && usuarioRepository.existsByPersonaId(persona.getId())) {
            throw new ReglaNegocioException("La persona con documento " + datos.numeroDocumento() + " ya tiene usuario");
        }
        persona.setNombres(datos.nombres().trim());
        persona.setApellidos(datos.apellidos().trim());
        persona.setTelefono(vacioANulo(datos.telefono()));
        persona.setCorreo(vacioANulo(datos.correo()));
        personaRepository.save(persona);

        Usuario usuario = new Usuario(persona, datos.nombreUsuario(), passwordEncoder.encode(datos.contrasena()));
        usuario.setRoles(datos.roles());
        return usuarioRepository.save(usuario);
    }

    /**
     * Actualiza datos personales, roles y estado. Un administrador no puede desactivarse
     * ni quitarse el rol de administrador a si mismo, para no quedar sin acceso.
     */
    @Transactional
    public Usuario actualizar(Long id, ActualizarUsuarioDto datos, Long idUsuarioActual) {
        Usuario usuario = obtener(id);
        if (id.equals(idUsuarioActual) && (!datos.activo() || !datos.roles().contains(Rol.ADMINISTRADOR))) {
            throw new ReglaNegocioException("No puede desactivarse ni quitarse el rol de administrador a si mismo");
        }
        Persona persona = usuario.getPersona();
        persona.setNombres(datos.nombres().trim());
        persona.setApellidos(datos.apellidos().trim());
        persona.setTelefono(vacioANulo(datos.telefono()));
        persona.setCorreo(vacioANulo(datos.correo()));
        usuario.setActivo(datos.activo());
        usuario.setRoles(datos.roles());
        return usuario;
    }

    @Transactional
    public void cambiarContrasena(Long id, String contrasena) {
        Usuario usuario = obtener(id);
        usuario.setContrasenaHash(passwordEncoder.encode(contrasena));
        usuario.desbloquear();
    }

    @Transactional
    public Usuario desbloquear(Long id) {
        Usuario usuario = obtener(id);
        usuario.desbloquear();
        return usuario;
    }

    private static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
