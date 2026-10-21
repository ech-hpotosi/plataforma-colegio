package co.edu.elencano.plataforma.estudiantes.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;

public interface EstudianteRepository extends JpaRepository<Estudiante, Long> {

    boolean existsByCodigoAndIdNot(String codigo, Long id);

    /** Estudiantes con ese numero de documento, sin importar el tipo (pudo pasar de registro civil a TI). */
    @Query("select e from Estudiante e where e.persona.numeroDocumento = :numero")
    List<Estudiante> buscarPorNumeroDocumento(@Param("numero") String numero);

    /** Busca por nombres, apellidos, documento o codigo. Texto vacio y estado nulo devuelven todos. */
    @Query(value = """
            select e from Estudiante e join fetch e.persona p
            where (:estado is null or e.estado = :estado)
              and (:texto = ''
               or lower(p.nombres) like concat('%', :texto, '%')
               or lower(p.apellidos) like concat('%', :texto, '%')
               or p.numeroDocumento like concat('%', :texto, '%')
               or lower(e.codigo) like concat('%', :texto, '%'))
            """,
            countQuery = """
            select count(e) from Estudiante e join e.persona p
            where (:estado is null or e.estado = :estado)
              and (:texto = ''
               or lower(p.nombres) like concat('%', :texto, '%')
               or lower(p.apellidos) like concat('%', :texto, '%')
               or p.numeroDocumento like concat('%', :texto, '%')
               or lower(e.codigo) like concat('%', :texto, '%'))
            """)
    Page<Estudiante> buscar(@Param("texto") String texto, @Param("estado") EstadoEstudiante estado, Pageable pageable);
}
