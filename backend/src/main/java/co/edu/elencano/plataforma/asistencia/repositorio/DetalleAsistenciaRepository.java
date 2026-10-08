package co.edu.elencano.plataforma.asistencia.repositorio;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.asistencia.entidad.DetalleAsistencia;

public interface DetalleAsistenciaRepository extends JpaRepository<DetalleAsistencia, Long> {

    /**
     * Horas por estudiante, asignatura y estado de los estudiantes que hoy estan en el grupo.
     * Se filtra por el grupo de la matricula para que un estudiante trasladado conserve sus faltas.
     */
    @Query("""
            select new co.edu.elencano.plataforma.asistencia.repositorio.ConteoInasistencia(
                   d.matricula.id, r.carga.asignatura.id, d.estado, sum(r.horas))
            from DetalleAsistencia d join d.registro r
            where d.matricula.grupo.id = :grupoId and d.matricula.estado = 'ACTIVA' and d.estado <> 'ASISTIO'
            group by d.matricula.id, r.carga.asignatura.id, d.estado
            """)
    List<ConteoInasistencia> contarDeGrupo(@Param("grupoId") Long grupoId);

    /** Faltas, permisos y retardos de una matricula, de la mas reciente a la mas antigua. */
    @Query("""
            select d from DetalleAsistencia d join fetch d.registro r join fetch r.carga c join fetch c.asignatura
            where d.matricula.id = :matriculaId and d.estado <> 'ASISTIO'
            order by r.fecha desc, c.asignatura.nombre
            """)
    List<DetalleAsistencia> listarNovedadesDeMatricula(@Param("matriculaId") Long matriculaId);

    @Query("""
            select d from DetalleAsistencia d join fetch d.registro r
            where d.matricula.id = :matriculaId and r.fecha = :fecha and d.estado = 'FALTA'
            """)
    List<DetalleAsistencia> listarFaltasSinJustificar(@Param("matriculaId") Long matriculaId,
                                                      @Param("fecha") LocalDate fecha);
}
