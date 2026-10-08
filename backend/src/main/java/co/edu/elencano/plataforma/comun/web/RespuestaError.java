package co.edu.elencano.plataforma.comun.web;

import java.util.Map;

/**
 * Cuerpo comun de las respuestas de error de la API.
 *
 * @param mensaje texto para mostrar al usuario
 * @param errores errores de validacion por campo (vacio si no aplica)
 */
public record RespuestaError(String mensaje, Map<String, String> errores) {

    public static RespuestaError de(String mensaje) {
        return new RespuestaError(mensaje, Map.of());
    }
}
