package co.edu.elencano.plataforma.estudiantes.web.dto;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/** Ficha completa del estudiante con sus acudientes (el principal primero). */
public record EstudianteDto(Long id, TipoDocumento tipoDocumento, String numeroDocumento, String nombres,
                            String apellidos, String telefono, String correo, String codigo,
                            LocalDate fechaNacimiento, Genero genero, String direccion, String eps,
                            String grupoSanguineo, String condicionDiscapacidad, boolean tienePiar,
                            String condicionesEspeciales, EstadoEstudiante estado,
                            List<AcudienteVinculadoDto> acudientes) {

    public static EstudianteDto de(Estudiante e) {
        Persona p = e.getPersona();
        List<AcudienteVinculadoDto> acudientes = e.getAcudientes().stream()
                .map(AcudienteVinculadoDto::de)
                .sorted(Comparator.comparing(AcudienteVinculadoDto::principal).reversed()
                        .thenComparing(AcudienteVinculadoDto::apellidos))
                .toList();
        return new EstudianteDto(e.getId(), p.getTipoDocumento(), p.getNumeroDocumento(), p.getNombres(),
                p.getApellidos(), p.getTelefono(), p.getCorreo(), e.getCodigo(), e.getFechaNacimiento(),
                e.getGenero(), e.getDireccion(), e.getEps(), e.getGrupoSanguineo(), e.getCondicionDiscapacidad(),
                e.isTienePiar(), e.getCondicionesEspeciales(), e.getEstado(), acudientes);
    }
}
