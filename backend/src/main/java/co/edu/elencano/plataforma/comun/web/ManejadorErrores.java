package co.edu.elencano.plataforma.comun.web;

import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;

/** Convierte las excepciones de los controladores en respuestas JSON con un mensaje claro. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<RespuestaError> validacion(MethodArgumentNotValidException ex) {
        Map<String, String> errores = new LinkedHashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errores.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        return ResponseEntity.badRequest().body(new RespuestaError("Hay datos invalidos en el formulario", errores));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<RespuestaError> cuerpoInvalido(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest().body(RespuestaError.de("La solicitud no tiene el formato esperado"));
    }

    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<RespuestaError> noEncontrado(RecursoNoEncontradoException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespuestaError.de(ex.getMessage()));
    }

    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<RespuestaError> reglaNegocio(ReglaNegocioException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(RespuestaError.de(ex.getMessage()));
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<RespuestaError> autenticacion(AuthenticationException ex) {
        String mensaje;
        if (ex instanceof LockedException) {
            mensaje = "La cuenta está bloqueada temporalmente por intentos fallidos. Intente más tarde";
        } else if (ex instanceof DisabledException) {
            mensaje = "El usuario está inactivo. Comuníquese con la secretaria del colegio";
        } else {
            mensaje = "Usuario o contraseña incorrectos";
        }
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(RespuestaError.de(mensaje));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<RespuestaError> accesoDenegado(AccessDeniedException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(RespuestaError.de("No tiene permiso para esta acción"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<RespuestaError> rutaNoExiste(NoResourceFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(RespuestaError.de("Recurso no encontrado"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<RespuestaError> errorInesperado(Exception ex) {
        log.error("Error no controlado", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(RespuestaError.de("Ocurrio un error inesperado. Intente de nuevo"));
    }
}
