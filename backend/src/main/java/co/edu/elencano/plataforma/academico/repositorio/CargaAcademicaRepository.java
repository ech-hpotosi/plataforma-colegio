package co.edu.elencano.plataforma.academico.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.academico.entidad.CargaAcademica;

public interface CargaAcademicaRepository extends JpaRepository<CargaAcademica, Long> {

    List<CargaAcademica> findByGrupoId(Long grupoId);

    boolean existsByGrupoId(Long grupoId);

    /** Indica si alguna asignatura del plan de un grado y anio ya tiene docente asignado en algun grupo. */
    @Query("""
            select count(c) > 0 from CargaAcademica c
            where c.grupo.anioLectivo.id = :anioId and c.grupo.grado.id = :gradoId and c.asignatura.id = :asignaturaId
            """)
    boolean asignaturaTieneCarga(@Param("anioId") Long anioId, @Param("gradoId") Long gradoId,
                                 @Param("asignaturaId") Long asignaturaId);
}
