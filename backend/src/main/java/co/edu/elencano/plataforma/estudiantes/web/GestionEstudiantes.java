package co.edu.elencano.plataforma.estudiantes.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/** Operaciones que modifican datos de estudiantes, acudientes o matriculas: administrador o secretaria. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'SECRETARIA')")
public @interface GestionEstudiantes {
}
