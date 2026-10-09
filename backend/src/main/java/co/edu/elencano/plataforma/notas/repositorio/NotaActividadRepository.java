package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.notas.entidad.NotaActividad;

public interface NotaActividadRepository extends JpaRepository<NotaActividad, Long> {

    @Query("""
            select n from NotaActividad n join fetch n.actividad a
            where a.carga.id = :cargaId and a.periodo.id = :periodoId
            """)
    List<NotaActividad> listarDeCargaYPeriodo(@Param("cargaId") Long cargaId, @Param("periodoId") Long periodoId);

    /** Todas las notas de las cargas del grupo, de todos los periodos, para el consolidado. */
    @Query("""
            select n from NotaActividad n join fetch n.actividad a join fetch a.periodo
            where a.carga.grupo.id = :grupoId
            """)
    List<NotaActividad> listarDeGrupo(@Param("grupoId") Long grupoId);
}
