package co.edu.elencano.plataforma.asistencia.repositorio;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.elencano.plataforma.asistencia.entidad.RegistroAsistencia;

public interface RegistroAsistenciaRepository extends JpaRepository<RegistroAsistencia, Long> {

    Optional<RegistroAsistencia> findByCargaIdAndFecha(Long cargaId, LocalDate fecha);
}
