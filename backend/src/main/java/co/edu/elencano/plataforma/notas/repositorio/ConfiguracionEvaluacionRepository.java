package co.edu.elencano.plataforma.notas.repositorio;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.notas.entidad.ConfiguracionEvaluacion;

public interface ConfiguracionEvaluacionRepository extends JpaRepository<ConfiguracionEvaluacion, Long> {

    Optional<ConfiguracionEvaluacion> findByAnioLectivoId(Long anioId);
}
