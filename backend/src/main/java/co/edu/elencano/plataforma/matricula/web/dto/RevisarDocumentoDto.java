package co.edu.elencano.plataforma.matricula.web.dto;

import co.edu.elencano.plataforma.matricula.entidad.EstadoDocumento;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RevisarDocumentoDto(
        @NotNull(message = "Indique si el documento se aprueba o se rechaza") EstadoDocumento estado,
        @Size(max = 300) String observacion) {
}
