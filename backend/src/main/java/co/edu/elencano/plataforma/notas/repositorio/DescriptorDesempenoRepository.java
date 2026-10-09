package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.notas.entidad.DescriptorDesempeno;

public interface DescriptorDesempenoRepository extends JpaRepository<DescriptorDesempeno, Long> {

    @Query("select x from DescriptorDesempeno x where x.carga.id = :cargaId and x.periodo.id = :periodoId")
    List<DescriptorDesempeno> findByCargaIdAndPeriodoId(@Param("cargaId") Long cargaId, @Param("periodoId") Long periodoId);

    @Query("select x from DescriptorDesempeno x where x.carga.grupo.id = :grupoId and x.periodo.id = :periodoId")
    List<DescriptorDesempeno> findByCargaGrupoIdAndPeriodoId(@Param("grupoId") Long grupoId, @Param("periodoId") Long periodoId);
}
