package co.edu.elencano.plataforma.matricula.repositorio;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.elencano.plataforma.matricula.entidad.EstadoSolicitud;
import co.edu.elencano.plataforma.matricula.entidad.SolicitudMatricula;

public interface SolicitudMatriculaRepository extends JpaRepository<SolicitudMatricula, Long> {

    @Query("select s from SolicitudMatricula s where s.solicitante.id = :usuarioId order by s.fechaCreacion desc")
    List<SolicitudMatricula> listarDeSolicitante(@Param("usuarioId") Long usuarioId);

    /** Solicitudes vigentes (no rechazadas) del mismo estudiante en el anio, para no duplicarlas. */
    @Query("""
            select count(s) > 0 from SolicitudMatricula s
            where s.anioLectivo.id = :anioId and s.numeroDocumento = :documento
              and s.estado <> 'RECHAZADA' and s.id <> :excluir
            """)
    boolean existeVigente(@Param("anioId") Long anioId, @Param("documento") String documento,
                          @Param("excluir") Long excluir);

    /** Busca por nombres, apellidos o documento del estudiante. Estado nulo devuelve todas menos los borradores. */
    @Query(value = """
            select s from SolicitudMatricula s
            where s.anioLectivo.id = :anioId
              and ((:estado is null and s.estado <> 'BORRADOR') or s.estado = :estado)
              and (:texto = ''
               or lower(s.nombres) like concat('%', :texto, '%')
               or lower(s.apellidos) like concat('%', :texto, '%')
               or s.numeroDocumento like concat('%', :texto, '%'))
            """,
            countQuery = """
            select count(s) from SolicitudMatricula s
            where s.anioLectivo.id = :anioId
              and ((:estado is null and s.estado <> 'BORRADOR') or s.estado = :estado)
              and (:texto = ''
               or lower(s.nombres) like concat('%', :texto, '%')
               or lower(s.apellidos) like concat('%', :texto, '%')
               or s.numeroDocumento like concat('%', :texto, '%'))
            """)
    Page<SolicitudMatricula> buscar(@Param("anioId") Long anioId, @Param("estado") EstadoSolicitud estado,
                                    @Param("texto") String texto, Pageable pageable);
}
