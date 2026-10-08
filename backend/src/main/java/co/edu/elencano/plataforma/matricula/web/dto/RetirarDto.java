package co.edu.elencano.plataforma.matricula.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RetirarDto(@NotBlank(message = "Ingrese el motivo del retiro") @Size(max = 300) String motivo) {
}
