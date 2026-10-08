package co.edu.elencano.plataforma.academico.web;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.security.access.prepost.PreAuthorize;

/** Marca las operaciones que modifican la estructura academica: solo administrador o coordinador academico. */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@PreAuthorize("hasAnyRole('ADMINISTRADOR', 'COORDINADOR_ACADEMICO')")
public @interface GestionAcademica {
}
