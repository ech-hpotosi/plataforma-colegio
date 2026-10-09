package co.edu.elencano.plataforma.notas.repositorio;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.notas.entidad.InformeEstudiante;

public interface InformeEstudianteRepository extends JpaRepository<InformeEstudiante, Long> {

    List<InformeEstudiante> findByCargaIdAndPeriodoId(Long cargaId, Long periodoId);
}
