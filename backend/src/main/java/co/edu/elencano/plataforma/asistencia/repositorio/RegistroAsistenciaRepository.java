package co.edu.elencano.plataforma.asistencia.repositorio;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.asistencia.entidad.RegistroAsistencia;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    Optional<RegistroAsistencia> findByCargaIdAndFecha(Long cargaId, LocalDate fecha);

    /** De las cargas indicadas, las que ya tienen asistencia en la fecha. */
    @Query("select r.carga.id from RegistroAsistencia r where r.fecha = :fecha and r.carga.id in :cargaIds")
    List<Long> listarCargasRegistradas(@Param("cargaIds") Collection<Long> cargaIds, @Param("fecha") LocalDate fecha);

    @Query("""
            select new co.edu.elencano.plataforma.asistencia.repositorio.ResumenRegistros(
                   r.carga.id, r.periodo.id, count(r), max(r.fecha))
            from RegistroAsistencia r
            where r.carga.id in :cargaIds
            group by r.carga.id, r.periodo.id
            """)
    List<ResumenRegistros> resumirDeCargas(@Param("cargaIds") Collection<Long> cargaIds);
}
