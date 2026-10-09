package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.notas.entidad.InformeEstudiante;

public interface InformeEstudianteRepository extends JpaRepository<InformeEstudiante, Long> {

    @Query("select x from InformeEstudiante x where x.carga.id = :cargaId and x.periodo.id = :periodoId")
    List<InformeEstudiante> findByCargaIdAndPeriodoId(@Param("cargaId") Long cargaId, @Param("periodoId") Long periodoId);

    @Query("select x from InformeEstudiante x where x.carga.grupo.id = :grupoId and x.periodo.id = :periodoId")
    List<InformeEstudiante> findByCargaGrupoIdAndPeriodoId(@Param("grupoId") Long grupoId, @Param("periodoId") Long periodoId);
}
