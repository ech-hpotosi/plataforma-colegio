package co.edu.elencano.plataforma.usuarios.servicio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.Rol;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import co.edu.elencano.plataforma.usuarios.entidad.Usuario;
import co.edu.elencano.plataforma.usuarios.repositorio.PersonaRepository;
import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

/**
 * Crea el primer administrador cuando la tabla de usuarios esta vacia.
 * La contrasena llega por variable de entorno para no dejarla escrita en el codigo ni en una migracion.
 */
@Component
public class AdministradorInicial implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdministradorInicial.class);

    private final UsuarioRepository usuarioRepository;
    private final PersonaRepository personaRepository;
    private final PasswordEncoder passwordEncoder;
    private final String nombreUsuario;
    private final String contrasena;

    public AdministradorInicial(UsuarioRepository usuarioRepository,
                                PersonaRepository personaRepository,
                                PasswordEncoder passwordEncoder,
                                @Value("${plataforma.admin-inicial.usuario:admin}") String nombreUsuario,
                                @Value("${plataforma.admin-inicial.contrasena:}") String contrasena) {
        this.usuarioRepository = usuarioRepository;
        this.personaRepository = personaRepository;
        this.passwordEncoder = passwordEncoder;
        this.nombreUsuario = nombreUsuario;
        this.contrasena = contrasena;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarioRepository.count() > 0) {
            return;
        }
        if (!StringUtils.hasText(contrasena)) {
            log.warn("No hay usuarios y no se definio ADMIN_CONTRASENA; no se crea el administrador inicial");
            return;
        }
        Persona persona = personaRepository.save(
                new Persona(TipoDocumento.CC, "0", "Administrador", "Plataforma"));
        Usuario admin = new Usuario(persona, nombreUsuario, passwordEncoder.encode(contrasena));
        admin.agregarRol(Rol.ADMINISTRADOR);
        usuarioRepository.save(admin);
        log.info("Administrador inicial '{}' creado", nombreUsuario);
    }
}
