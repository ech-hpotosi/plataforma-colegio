package co.edu.elencano.plataforma.matricula.repositorio;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.matricula.entidad.EstadoMatricula;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    @Query("select m from Matricula m where m.estudiante.id = :estudianteId order by m.anioLectivo.anio desc")
    List<Matricula> listarDeEstudiante(@Param("estudianteId") Long estudianteId);

    Optional<Matricula> findByEstudianteIdAndAnioLectivoId(Long estudianteId, Long anioLectivoId);

    /** Estudiantes con matricula activa en el grupo, ordenados por apellidos y nombres. */
    @Query("""
            select m from Matricula m join fetch m.estudiante e join fetch e.persona p
            where m.grupo.id = :grupoId and m.estado = 'ACTIVA'
            order by p.apellidos, p.nombres
            """)
    List<Matricula> listarActivasDeGrupo(@Param("grupoId") Long grupoId);

    long countByGrupoIdAndEstado(Long grupoId, EstadoMatricula estado);
}
