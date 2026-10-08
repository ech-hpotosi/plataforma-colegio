package co.edu.elencano.plataforma.comun.web;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Endpoint publico para verificar que la API esta en linea.
 * Lo usan el frontend en desarrollo y el monitor de disponibilidad en produccion.
 */
@RestController
@RequestMapping("/api/salud")
public class SaludController {

    @GetMapping
    public Map<String, Object> salud() {
        return Map.of(
                "estado", "OK",
                "aplicacion", "plataforma-colegio",
                "fecha", LocalDateTime.now().toString());
    }
}
