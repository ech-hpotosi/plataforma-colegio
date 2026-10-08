package co.edu.elencano.plataforma.usuarios.web;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import co.edu.elencano.plataforma.comun.web.Pagina;
import co.edu.elencano.plataforma.usuarios.seguridad.UsuarioAutenticado;
import co.edu.elencano.plataforma.usuarios.servicio.UsuarioService;
import co.edu.elencano.plataforma.usuarios.web.dto.ActualizarUsuarioDto;
import co.edu.elencano.plataforma.usuarios.web.dto.CambiarContrasenaDto;
import co.edu.elencano.plataforma.usuarios.web.dto.CrearUsuarioDto;
import co.edu.elencano.plataforma.usuarios.web.dto.UsuarioDto;
import jakarta.validation.Valid;

/** Administracion de usuarios. Solo para el rol ADMINISTRADOR. */
@RestController
@RequestMapping("/api/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public Pagina<UsuarioDto> buscar(@RequestParam(defaultValue = "") String buscar,
                                     @RequestParam(defaultValue = "0") int pagina,
                                     @RequestParam(defaultValue = "20") int tamano) {
        return Pagina.de(usuarioService.buscar(buscar, pagina, tamano), UsuarioDto::de);
    }

    @GetMapping("/{id}")
    public UsuarioDto obtener(@PathVariable Long id) {
        return UsuarioDto.de(usuarioService.obtener(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioDto crear(@Valid @RequestBody CrearUsuarioDto datos) {
        return UsuarioDto.de(usuarioService.crear(datos));
    }

    @PutMapping("/{id}")
    public UsuarioDto actualizar(@PathVariable Long id,
                                 @Valid @RequestBody ActualizarUsuarioDto datos,
                                 @AuthenticationPrincipal UsuarioAutenticado actual) {
        return UsuarioDto.de(usuarioService.actualizar(id, datos, actual.getId()));
    }

    @PutMapping("/{id}/contrasena")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void cambiarContrasena(@PathVariable Long id, @Valid @RequestBody CambiarContrasenaDto datos) {
        usuarioService.cambiarContrasena(id, datos.contrasena());
    }

    @PostMapping("/{id}/desbloquear")
    public UsuarioDto desbloquear(@PathVariable Long id) {
        return UsuarioDto.de(usuarioService.desbloquear(id));
    }
}
