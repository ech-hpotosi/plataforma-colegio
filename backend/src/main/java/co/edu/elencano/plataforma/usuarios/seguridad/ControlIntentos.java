package co.edu.elencano.plataforma.usuarios.seguridad;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.elencano.plataforma.usuarios.repositorio.UsuarioRepository;

/**
 * Registra los intentos de inicio de sesion y bloquea la cuenta tras varios fallos seguidos.
 * Va en su propia clase para que cada registro se guarde en su propia transaccion,
 * aunque despues el login termine en error.
 */
@Component
public class ControlIntentos {

    private final UsuarioRepository usuarioRepository;
    private final int maximoIntentos;
    private final int minutosBloqueo;

    public ControlIntentos(UsuarioRepository usuarioRepository,
                           @Value("${plataforma.seguridad.maximo-intentos:5}") int maximoIntentos,
                           @Value("${plataforma.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.usuarioRepository = usuarioRepository;
        this.maximoIntentos = maximoIntentos;
        this.minutosBloqueo = minutosBloqueo;
    }

    @Transactional
    public void registrarFallo(String nombreUsuario) {
        LocalDateTime ahora = LocalDateTime.now();
        usuarioRepository.findByNombreUsuario(nombreUsuario).ifPresent(usuario ->
                usuario.registrarIntentoFallido(maximoIntentos, ahora.plusMinutes(minutosBloqueo)));
    }

    @Transactional
    public void registrarExito(String nombreUsuario) {
        usuarioRepository.findByNombreUsuario(nombreUsuario)
                .ifPresent(usuario -> usuario.registrarAccesoExitoso(LocalDateTime.now()));
    }
}
