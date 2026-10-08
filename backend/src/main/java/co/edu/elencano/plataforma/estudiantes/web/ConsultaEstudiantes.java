package co.edu.elencano.plataforma.estudiantes.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Consulta de datos de estudiantes (son menores de edad): solo directivos y secretaria.
 * Docentes, estudiantes y acudientes tendran consultas propias limitadas a lo suyo.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'RECTOR', 'COORDINADOR_ACADEMICO', 'SECRETARIA')")
public @interface ConsultaEstudiantes {
}
