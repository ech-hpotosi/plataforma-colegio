package co.edu.elencano.plataforma.usuarios.servicio;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import co.edu.elencano.plataforma.usuarios.seguridad.ControlIntentos;

/** Verifica usuario y contrasena y lleva la cuenta de intentos fallidos. */
@Service
public class AutenticacionService {

    private final AuthenticationManager authenticationManager;
    private final ControlIntentos controlIntentos;

    public AutenticacionService(AuthenticationManager authenticationManager, ControlIntentos controlIntentos) {
        this.authenticationManager = authenticationManager;
        this.controlIntentos = controlIntentos;
    }

    /**
     * Autentica al usuario. Lanza BadCredentialsException, LockedException o DisabledException
     * si no se puede iniciar sesion; el manejador de errores las convierte en 401.
     */
    public Authentication autenticar(String nombreUsuario, String contrasena) {
        try {
            Authentication autenticacion = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(nombreUsuario, contrasena));
            controlIntentos.registrarExito(nombreUsuario);
            return autenticacion;
        } catch (BadCredentialsException ex) {
            controlIntentos.registrarFallo(nombreUsuario);
            throw ex;
        }
    }
}
