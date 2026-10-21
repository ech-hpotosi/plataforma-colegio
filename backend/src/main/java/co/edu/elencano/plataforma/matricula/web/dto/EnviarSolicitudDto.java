package co.edu.elencano.plataforma.matricula.web.dto;

import jakarta.validation.constraints.AssertTrue;

/** Al enviar, el acudiente debe autorizar el tratamiento de los datos del estudiante (Ley 1581 de 2012). */
public record EnviarSolicitudDto(
        @AssertTrue(message = "Debe autorizar el tratamiento de datos para enviar la solicitud") boolean autorizaDatos) {
}
