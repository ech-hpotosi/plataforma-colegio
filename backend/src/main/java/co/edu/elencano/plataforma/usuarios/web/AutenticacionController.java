package co.edu.elencano.plataforma.usuarios.web;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import co.edu.elencano.plataforma.usuarios.servicio.AutenticacionService;
import co.edu.elencano.plataforma.usuarios.web.dto.LoginDto;
import co.edu.elencano.plataforma.usuarios.web.dto.UsuarioActualDto;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

/**
 * Inicio de sesion y datos del usuario actual.
 * El cierre de sesion (POST /api/auth/logout) lo atiende Spring Security, ver SeguridadConfig.
 */
@RestController
@RequestMapping("/api")
public class AutenticacionController {

    private final AutenticacionService autenticacionService;
    private final SecurityContextRepository securityContextRepository;

    public AutenticacionController(AutenticacionService autenticacionService,
                                   SecurityContextRepository securityContextRepository) {
        this.autenticacionService = autenticacionService;
        this.securityContextRepository = securityContextRepository;
    }

    @PostMapping("/auth/login")
    public UsuarioActualDto login(@Valid @RequestBody LoginDto datos,
                                  HttpServletRequest request,
                                  HttpServletResponse response) {
        Authentication autenticacion = autenticacionService.autenticar(datos.nombreUsuario(), datos.contrasena());

        // Cambia el id de sesion al autenticarse para evitar ataques de fijacion de sesion
        request.getSession(true);
        request.changeSessionId();

        SecurityContext contexto = SecurityContextHolder.createEmptyContext();
        contexto.setAuthentication(autenticacion);
        SecurityContextHolder.setContext(contexto);
        securityContextRepository.saveContext(contexto, request, response);

        return UsuarioActualDto.de((UsuarioAutenticado) autenticacion.getPrincipal());
    }

    @GetMapping("/yo")
    public UsuarioActualDto yo(@AuthenticationPrincipal UsuarioAutenticado usuario) {
        return UsuarioActualDto.de(usuario);
    }
}
