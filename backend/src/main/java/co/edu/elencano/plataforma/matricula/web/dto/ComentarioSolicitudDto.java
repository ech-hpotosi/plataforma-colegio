package co.edu.elencano.plataforma.matricula.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Motivo con el que la secretaria devuelve o rechaza una solicitud. Lo lee el acudiente. */
public record ComentarioSolicitudDto(
        @NotBlank(message = "Escriba el motivo para el acudiente") @Size(max = 500) String comentario) {
}
