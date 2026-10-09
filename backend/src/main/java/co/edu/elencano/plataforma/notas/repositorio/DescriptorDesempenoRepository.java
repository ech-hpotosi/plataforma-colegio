package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.notas.entidad.DescriptorDesempeno;

public interface DescriptorDesempenoRepository extends JpaRepository<DescriptorDesempeno, Long> {

    List<DescriptorDesempeno> findByCargaIdAndPeriodoId(Long cargaId, Long periodoId);
}
