package co.edu.elencano.plataforma.estudiantes.servicio;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import co.edu.elencano.plataforma.estudiantes.entidad.Acudiente;
import co.edu.elencano.plataforma.estudiantes.entidad.EstadoEstudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.EstudianteAcudiente;
import co.edu.elencano.plataforma.estudiantes.repositorio.AcudienteRepository;
import co.edu.elencano.plataforma.estudiantes.repositorio.EstudianteRepository;
import co.edu.elencano.plataforma.estudiantes.web.dto.AcudienteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.EstudianteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarAcudienteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarEstudianteDto;
import co.edu.elencano.plataforma.comun.excepcion.RecursoNoEncontradoException;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.usuarios.entidad.Persona;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;
import co.edu.elencano.plataforma.usuarios.repositorio.PersonaRepository;

/**
 * Ficha de estudiantes y sus acudientes. Si la persona ya existe (mismo documento) se reutiliza,
 * porque una misma persona puede ser acudiente, docente o funcionario a la vez.
 */
@Service
public class EstudianteService {

    private final EstudianteRepository estudianteRepository;
    private final AcudienteRepository acudienteRepository;
    private final PersonaRepository personaRepository;

    public EstudianteService(EstudianteRepository estudianteRepository, AcudienteRepository acudienteRepository,
                             PersonaRepository personaRepository) {
        this.estudianteRepository = estudianteRepository;
        this.acudienteRepository = acudienteRepository;
        this.personaRepository = personaRepository;
    }

    @Transactional(readOnly = true)
    public Page<Estudiante> buscar(String texto, EstadoEstudiante estado, int pagina, int tamano) {
        String filtro = texto == null ? "" : texto.trim().toLowerCase();
        PageRequest paginacion = PageRequest.of(Math.max(pagina, 0), Math.clamp(tamano, 1, 100),
                Sort.by("persona.apellidos", "persona.nombres"));
        return estudianteRepository.buscar(filtro, estado, paginacion);
    }

    @Transactional(readOnly = true)
    public Estudiante obtener(Long id) {
        return estudianteRepository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("No existe el estudiante " + id));
    }

    @Transactional(readOnly = true)
    public EstudianteDto detalle(Long id) {
        return EstudianteDto.de(obtener(id));
    }

    @Transactional
    public EstudianteDto crear(GuardarEstudianteDto datos) {
        Persona persona = personaRepository
                .findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), datos.numeroDocumento())
                .orElseGet(() -> new Persona(datos.tipoDocumento(), datos.numeroDocumento(),
                        datos.nombres(), datos.apellidos()));
        if (persona.getId() != null && estudianteRepository.existsById(persona.getId())) {
            throw new ReglaNegocioException("Ya existe un estudiante con el documento " + datos.numeroDocumento());
        }
        aplicarPersona(persona, datos.nombres(), datos.apellidos(), datos.telefono(), datos.correo());
        personaRepository.save(persona);
        Estudiante estudiante = new Estudiante(persona);
        aplicar(estudiante, datos);
        return EstudianteDto.de(estudianteRepository.save(estudiante));
    }

    @Transactional
    public EstudianteDto actualizar(Long id, GuardarEstudianteDto datos) {
        Estudiante estudiante = obtener(id);
        Persona persona = estudiante.getPersona();
        Optional<Persona> conDocumento = personaRepository
                .findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), datos.numeroDocumento());
        if (conDocumento.isPresent() && !conDocumento.get().getId().equals(id)) {
            throw new ReglaNegocioException("El documento " + datos.numeroDocumento() + " ya pertenece a otra persona");
        }
        persona.cambiarDocumento(datos.tipoDocumento(), datos.numeroDocumento());
        aplicarPersona(persona, datos.nombres(), datos.apellidos(), datos.telefono(), datos.correo());
        aplicar(estudiante, datos);
        return EstudianteDto.de(estudiante);
    }

    /** Vincula un acudiente al estudiante, creando la persona y el acudiente si no existen. */
    @Transactional
    public EstudianteDto vincularAcudiente(Long estudianteId, GuardarAcudienteDto datos) {
        Estudiante estudiante = obtener(estudianteId);
        Persona persona = personaRepository
                .findByTipoDocumentoAndNumeroDocumento(datos.tipoDocumento(), datos.numeroDocumento())
                .orElseGet(() -> personaRepository.save(new Persona(datos.tipoDocumento(), datos.numeroDocumento(),
                        datos.nombres(), datos.apellidos())));
        if (persona.getId().equals(estudianteId)) {
            throw new ReglaNegocioException("El estudiante no puede ser su propio acudiente");
        }
        if (estudiante.vinculoCon(persona.getId()) != null) {
            throw new ReglaNegocioException(persona.getNombreCompleto() + " ya es acudiente de este estudiante");
        }
        aplicarPersona(persona, datos.nombres(), datos.apellidos(), datos.telefono(), datos.correo());
        Acudiente acudiente = acudienteRepository.findById(persona.getId())
                .orElseGet(() -> acudienteRepository.save(new Acudiente(persona)));
        acudiente.setOcupacion(vacioANulo(datos.ocupacion()));
        EstudianteAcudiente vinculo = estudiante.vincular(acudiente, datos.parentesco());
        // El primer acudiente queda como principal aunque no se marque
        if (datos.principal() || estudiante.getAcudientes().size() == 1) {
            estudiante.marcarPrincipal(vinculo);
        }
        return EstudianteDto.de(estudiante);
    }

    @Transactional
    public EstudianteDto actualizarAcudiente(Long estudianteId, Long acudienteId, GuardarAcudienteDto datos) {
        Estudiante estudiante = obtener(estudianteId);
        EstudianteAcudiente vinculo = obtenerVinculo(estudiante, acudienteId);
        Persona persona = vinculo.getAcudiente().getPersona();
        aplicarPersona(persona, datos.nombres(), datos.apellidos(), datos.telefono(), datos.correo());
        vinculo.getAcudiente().setOcupacion(vacioANulo(datos.ocupacion()));
        vinculo.setParentesco(datos.parentesco());
        if (datos.principal()) {
            estudiante.marcarPrincipal(vinculo);
        }
        return EstudianteDto.de(estudiante);
    }

    /** Quita el vinculo. Si era el principal, el siguiente acudiente pasa a ser el principal. */
    @Transactional
    public EstudianteDto desvincularAcudiente(Long estudianteId, Long acudienteId) {
        Estudiante estudiante = obtener(estudianteId);
        EstudianteAcudiente vinculo = obtenerVinculo(estudiante, acudienteId);
        estudiante.desvincular(vinculo);
        if (vinculo.isPrincipal() && !estudiante.getAcudientes().isEmpty()) {
            estudiante.marcarPrincipal(estudiante.getAcudientes().getFirst());
        }
        return EstudianteDto.de(estudiante);
    }

    /** Busca una persona por documento para precargar el formulario de acudiente. */
    @Transactional(readOnly = true)
    public AcudienteDto buscarPersonaPorDocumento(TipoDocumento tipo, String numero) {
        Persona persona = personaRepository.findByTipoDocumentoAndNumeroDocumento(tipo, numero)
                .orElseThrow(() -> new RecursoNoEncontradoException("No hay ninguna persona con ese documento"));
        String ocupacion = acudienteRepository.findById(persona.getId()).map(Acudiente::getOcupacion).orElse(null);
        return AcudienteDto.de(persona, ocupacion);
    }

    private static EstudianteAcudiente obtenerVinculo(Estudiante estudiante, Long acudienteId) {
        EstudianteAcudiente vinculo = estudiante.vinculoCon(acudienteId);
        if (vinculo == null) {
            throw new RecursoNoEncontradoException("La persona " + acudienteId + " no es acudiente de este estudiante");
        }
        return vinculo;
    }

    private void aplicar(Estudiante estudiante, GuardarEstudianteDto datos) {
        String codigo = vacioANulo(datos.codigo());
        Long id = estudiante.getId() == null ? 0L : estudiante.getId();
        if (codigo != null && estudianteRepository.existsByCodigoAndIdNot(codigo, id)) {
            throw new ReglaNegocioException("El código " + codigo + " ya está asignado a otro estudiante");
        }
        estudiante.setCodigo(codigo);
        estudiante.setFechaNacimiento(datos.fechaNacimiento());
        estudiante.setGenero(datos.genero());
        estudiante.setDireccion(vacioANulo(datos.direccion()));
        estudiante.setEps(vacioANulo(datos.eps()));
        estudiante.setGrupoSanguineo(vacioANulo(datos.grupoSanguineo()));
        estudiante.setCondicionDiscapacidad(vacioANulo(datos.condicionDiscapacidad()));
        estudiante.setTienePiar(datos.tienePiar());
        estudiante.setCondicionesEspeciales(vacioANulo(datos.condicionesEspeciales()));
    }

    private static void aplicarPersona(Persona persona, String nombres, String apellidos, String telefono,
                                       String correo) {
        persona.setNombres(nombres.trim());
        persona.setApellidos(apellidos.trim());
        persona.setTelefono(vacioANulo(telefono));
        persona.setCorreo(vacioANulo(correo));
    }

    private static String vacioANulo(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }
}
