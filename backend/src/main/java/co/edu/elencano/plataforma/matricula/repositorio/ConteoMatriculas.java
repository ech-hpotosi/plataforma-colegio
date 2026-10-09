package co.edu.elencano.plataforma.matricula.repositorio;

/** Estudiantes con matricula activa en un grupo. */
public record ConteoMatriculas(Long grupoId, Long estudiantes) {
}
