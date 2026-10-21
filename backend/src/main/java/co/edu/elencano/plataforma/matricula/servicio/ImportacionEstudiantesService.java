package co.edu.elencano.plataforma.matricula.servicio;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import co.edu.elencano.plataforma.academico.entidad.AnioLectivo;
import co.edu.elencano.plataforma.academico.entidad.Grado;
import co.edu.elencano.plataforma.academico.entidad.Grupo;
import co.edu.elencano.plataforma.academico.entidad.Sede;
import co.edu.elencano.plataforma.academico.repositorio.GrupoRepository;
import co.edu.elencano.plataforma.academico.servicio.AnioLectivoService;
import co.edu.elencano.plataforma.academico.servicio.GradoService;
import co.edu.elencano.plataforma.academico.servicio.SedeService;
import co.edu.elencano.plataforma.comun.excepcion.ReglaNegocioException;
import co.edu.elencano.plataforma.estudiantes.entidad.Estudiante;
import co.edu.elencano.plataforma.estudiantes.entidad.Genero;
import co.edu.elencano.plataforma.estudiantes.entidad.Parentesco;
import co.edu.elencano.plataforma.estudiantes.servicio.EstudianteService;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarAcudienteDto;
import co.edu.elencano.plataforma.estudiantes.web.dto.GuardarEstudianteDto;
import co.edu.elencano.plataforma.matricula.entidad.Matricula;
import co.edu.elencano.plataforma.matricula.repositorio.MatriculaRepository;
import co.edu.elencano.plataforma.matricula.servicio.LectorExcel.Fila;
import co.edu.elencano.plataforma.matricula.web.dto.ResultadoImportacionDto;
import co.edu.elencano.plataforma.usuarios.entidad.TipoDocumento;

/**
 * Carga inicial de estudiantes desde Excel (los que hoy estan en OzetaRed, el SIMAT o una hoja propia).
 * Primero se revisa el archivo sin guardar nada; al confirmar, cada fila valida crea o actualiza la ficha
 * del estudiante, su matricula en el anio con el grupo y, si viene, el acudiente. Cada fila se guarda en su
 * propia transaccion, asi que una fila con problemas no impide cargar las demas, y volver a cargar el mismo
 * archivo no duplica nada.
 */
@Service
public class ImportacionEstudiantesService {

    static final int MAXIMO_FILAS = 3000;

    /** Columnas de la plantilla, en orden, con su titulo y los otros nombres con que se reconocen. */
    private static final Map<String, List<String>> COLUMNAS = new LinkedHashMap<>();

    static {
        COLUMNAS.put("tipoDocumento", List.of("Tipo de documento", "tipo documento", "tipodoc", "tipo doc", "tipo_documento", "tipo identificacion"));
        COLUMNAS.put("numeroDocumento", List.of("Número de documento", "documento", "doc", "nro documento", "numero_documento", "identificacion", "numero identificacion", "nuip"));
        COLUMNAS.put("nombres", List.of("Nombres", "nombre"));
        COLUMNAS.put("nombre1", List.of("Primer nombre", "nombre1"));
        COLUMNAS.put("nombre2", List.of("Segundo nombre", "nombre2"));
        COLUMNAS.put("apellidos", List.of("Apellidos", "apellido"));
        COLUMNAS.put("apellido1", List.of("Primer apellido", "apellido1"));
        COLUMNAS.put("apellido2", List.of("Segundo apellido", "apellido2"));
        COLUMNAS.put("fechaNacimiento", List.of("Fecha de nacimiento", "fecha nacimiento", "fecha_nacimiento", "fecha nac", "fechanac"));
        COLUMNAS.put("genero", List.of("Género", "sexo"));
        COLUMNAS.put("sede", List.of("Sede"));
        COLUMNAS.put("grado", List.of("Grado"));
        COLUMNAS.put("grupo", List.of("Grupo", "curso"));
        COLUMNAS.put("codigo", List.of("Código", "codigo interno", "codigo estudiante"));
        COLUMNAS.put("direccion", List.of("Dirección", "direccion residencia", "vereda"));
        COLUMNAS.put("telefono", List.of("Teléfono", "celular"));
        COLUMNAS.put("eps", List.of("EPS", "eps o sisben"));
        COLUMNAS.put("acudienteTipoDocumento", List.of("Tipo de documento del acudiente", "acudiente tipo documento"));
        COLUMNAS.put("acudienteDocumento", List.of("Documento del acudiente", "acudiente documento", "documento acudiente"));
        COLUMNAS.put("acudienteNombres", List.of("Nombres del acudiente", "acudiente nombres", "nombre acudiente"));
        COLUMNAS.put("acudienteApellidos", List.of("Apellidos del acudiente", "acudiente apellidos", "apellidos acudiente"));
        COLUMNAS.put("acudienteTelefono", List.of("Teléfono del acudiente", "acudiente telefono", "celular acudiente", "telefono acudiente"));
        COLUMNAS.put("parentesco", List.of("Parentesco", "parentesco acudiente"));
    }

    /** Columnas que van en la plantilla (las de nombre y apellido partidos solo se reconocen al leer). */
    private static final List<String> COLUMNAS_PLANTILLA = List.of("tipoDocumento", "numeroDocumento", "nombres",
            "apellidos", "fechaNacimiento", "genero", "sede", "grado", "grupo", "codigo", "direccion", "telefono", "eps",
            "acudienteTipoDocumento", "acudienteDocumento", "acudienteNombres", "acudienteApellidos",
            "acudienteTelefono", "parentesco");

    private static final List<DateTimeFormatter> FORMATOS_FECHA = List.of(
            DateTimeFormatter.ISO_LOCAL_DATE, DateTimeFormatter.ofPattern("d/M/uuuu"),
            DateTimeFormatter.ofPattern("d-M-uuuu"), DateTimeFormatter.ofPattern("uuuu/M/d"));

    private final AnioLectivoService anioService;
    private final SedeService sedeService;
    private final GradoService gradoService;
    private final GrupoRepository grupoRepository;
    private final EstudianteService estudianteService;
    private final MatriculaService matriculaService;
    private final MatriculaRepository matriculaRepository;
    private final TransactionTemplate transaccion;

    public ImportacionEstudiantesService(AnioLectivoService anioService, SedeService sedeService,
                                         GradoService gradoService, GrupoRepository grupoRepository,
                                         EstudianteService estudianteService, MatriculaService matriculaService,
                                         MatriculaRepository matriculaRepository,
                                         PlatformTransactionManager transactionManager) {
        this.anioService = anioService;
        this.sedeService = sedeService;
        this.gradoService = gradoService;
        this.grupoRepository = grupoRepository;
        this.estudianteService = estudianteService;
        this.matriculaService = matriculaService;
        this.matriculaRepository = matriculaRepository;
        this.transaccion = new TransactionTemplate(transactionManager);
    }

    /** Fila ya interpretada. Si tiene errores no se guarda. */
    private record Registro(int fila, GuardarEstudianteDto estudiante, Sede sede, Grado grado, Grupo grupo,
                            GuardarAcudienteDto acudiente, List<String> errores, List<String> avisos) {
    }

    /** Revisa el archivo; si confirmar es verdadero, ademas guarda las filas sin errores. */
    public ResultadoImportacionDto importar(Long anioId, InputStream contenido, boolean confirmar) {
        AnioLectivo anio = anioService.obtenerAbierto(anioId);
        List<Fila> filas = LectorExcel.leer(contenido, COLUMNAS, MAXIMO_FILAS);
        if (filas.isEmpty()) {
            throw new ReglaNegocioException("El archivo no tiene estudiantes debajo del encabezado");
        }
        Catalogo catalogo = transaccion.execute(estado -> new Catalogo(anio));
        Set<String> documentosVistos = new HashSet<>();
        List<ResultadoImportacionDto.Fila> resultado = new ArrayList<>();
        int creados = 0;
        int actualizados = 0;
        int conErrores = 0;
        for (Fila fila : filas) {
            Registro registro = interpretar(fila, catalogo);
            String documento = registro.estudiante() == null ? fila.valor("numeroDocumento")
                    : registro.estudiante().numeroDocumento();
            if (documento != null && !documentosVistos.add(documento)) {
                registro.errores().add("El documento " + documento + " está repetido en el archivo");
            }
            boolean existe = registro.estudiante() != null
                    && estudianteService.buscarPorNumeroDocumento(registro.estudiante().numeroDocumento()).isPresent();
            String accion = existe ? "ACTUALIZAR" : "CREAR";
            if (registro.errores().isEmpty() && confirmar) {
                try {
                    transaccion.executeWithoutResult(estado -> guardar(registro, anio));
                } catch (ReglaNegocioException e) {
                    registro.errores().add(e.getMessage());
                }
            }
            if (!registro.errores().isEmpty()) {
                accion = "ERROR";
                conErrores++;
            } else if (existe) {
                actualizados++;
            } else {
                creados++;
            }
            String nombre = registro.estudiante() == null ? null
                    : registro.estudiante().nombres() + " " + registro.estudiante().apellidos();
            String grupo = registro.grupo() == null ? null : registro.grupo().getNombre();
            resultado.add(new ResultadoImportacionDto.Fila(fila.numero(), documento, nombre,
                    registro.grado() == null ? fila.valor("grado") : registro.grado().getNombre(), grupo, accion,
                    registro.errores(), registro.avisos()));
        }
        return new ResultadoImportacionDto(anio.getAnio(), filas.size(), creados, actualizados, conErrores,
                confirmar, resultado);
    }

    /** Archivo de Excel con los encabezados, una fila de ejemplo y una hoja con los valores validos. */
    @Transactional(readOnly = true)
    public byte[] plantilla() {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Estudiantes");
            Row encabezado = hoja.createRow(0);
            Row ejemplo = hoja.createRow(1);
            List<String> valoresEjemplo = List.of("TI", "1085123456", "Luna Sofía", "Jojoa Chasoy", "2015-04-02",
                    "F", sedeService.listar().stream().findFirst().map(Sede::getNombre).orElse("Sede principal"),
                    "Sexto", "6-01", "", "Vereda Santa Lucía", "", "Emssanar", "CC", "27123456", "Rosa Elena",
                    "Jojoa Chasoy", "3104567890", "Madre");
            for (int i = 0; i < COLUMNAS_PLANTILLA.size(); i++) {
                encabezado.createCell(i).setCellValue(COLUMNAS.get(COLUMNAS_PLANTILLA.get(i)).getFirst());
                ejemplo.createCell(i).setCellValue(valoresEjemplo.get(i));
                hoja.setColumnWidth(i, 20 * 256);
            }
            Sheet ayuda = libro.createSheet("Valores válidos");
            List<String[]> lineas = new ArrayList<>();
            lineas.add(new String[] {"Columna", "Valores"});
            lineas.add(new String[] {"Tipo de documento", "RC, TI, CC, CE, PPT"});
            lineas.add(new String[] {"Fecha de nacimiento", "aaaa-mm-dd o dd/mm/aaaa"});
            lineas.add(new String[] {"Género", "F o M"});
            lineas.add(new String[] {"Parentesco", "Madre, Padre, Abuelo, Tío, Hermano, Tutor, Otro"});
            lineas.add(new String[] {"Grado", "Nombre (Sexto) o número (6). Transición es 0"});
            lineas.add(new String[] {"Grupo", "Nombre del grupo creado en Estructura académica (opcional)"});
            sedeService.listar().forEach(s -> lineas.add(new String[] {"Sede", s.getNombre()}));
            gradoService.listar().forEach(g -> lineas.add(new String[] {"Grado", g.getNombre()}));
            for (int i = 0; i < lineas.size(); i++) {
                Row fila = ayuda.createRow(i);
                fila.createCell(0).setCellValue(lineas.get(i)[0]);
                fila.createCell(1).setCellValue(lineas.get(i)[1]);
            }
            ayuda.setColumnWidth(0, 24 * 256);
            ayuda.setColumnWidth(1, 60 * 256);
            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void guardar(Registro registro, AnioLectivo anio) {
        GuardarEstudianteDto datos = registro.estudiante();
        Optional<Estudiante> existente = estudianteService.buscarPorNumeroDocumento(datos.numeroDocumento());
        Estudiante estudiante;
        if (existente.isPresent()) {
            estudiante = existente.get();
            estudianteService.actualizar(estudiante.getId(), conservarDatos(datos, estudiante));
        } else {
            estudiante = estudianteService.obtener(estudianteService.crear(datos).id());
        }
        Long grupoId = registro.grupo() == null ? null : registro.grupo().getId();
        Optional<Matricula> matricula = matriculaRepository.findByEstudianteIdAndAnioLectivoId(estudiante.getId(),
                anio.getId());
        if (matricula.isPresent() && matricula.get().estaActiva()) {
            Grupo actual = matricula.get().getGrupo();
            if (grupoId != null && (actual == null || !actual.getId().equals(grupoId))) {
                matriculaService.cambiarGrupo(matricula.get().getId(), grupoId);
            }
        } else {
            matriculaService.matricular(estudiante.getId(), anio.getId(), grupoId);
        }
        GuardarAcudienteDto acudiente = registro.acudiente();
        if (acudiente != null) {
            boolean yaVinculado = estudiante.getAcudientes().stream().anyMatch(v ->
                    v.getAcudiente().getPersona().getNumeroDocumento().equals(acudiente.numeroDocumento()));
            if (!yaVinculado) {
                estudianteService.vincularAcudiente(estudiante.getId(), acudiente);
            }
        }
    }

    /** Lo que el archivo trae vacio conserva lo que ya tenia la ficha. */
    private static GuardarEstudianteDto conservarDatos(GuardarEstudianteDto d, Estudiante e) {
        return new GuardarEstudianteDto(d.tipoDocumento(), d.numeroDocumento(), d.nombres(), d.apellidos(),
                primero(d.telefono(), e.getPersona().getTelefono()), e.getPersona().getCorreo(),
                primero(d.codigo(), e.getCodigo()), d.fechaNacimiento(), d.genero(),
                primero(d.direccion(), e.getDireccion()), primero(d.eps(), e.getEps()), e.getGrupoSanguineo(),
                e.getCondicionDiscapacidad(), e.isTienePiar(), e.getCondicionesEspeciales());
    }

    private static String primero(String valor, String porDefecto) {
        return valor != null ? valor : porDefecto;
    }

    private Registro interpretar(Fila fila, Catalogo catalogo) {
        List<String> errores = new ArrayList<>();
        List<String> avisos = new ArrayList<>();

        TipoDocumento tipo = tipoDocumento(fila.valor("tipoDocumento"), "Tipo de documento", errores);
        String documento = documento(fila.valor("numeroDocumento"), "Número de documento", errores);
        String nombres = unir(fila.valor("nombres"), fila.valor("nombre1"), fila.valor("nombre2"));
        String apellidos = unir(fila.valor("apellidos"), fila.valor("apellido1"), fila.valor("apellido2"));
        if (nombres == null) {
            errores.add("Faltan los nombres");
        }
        if (apellidos == null) {
            errores.add("Faltan los apellidos");
        }
        LocalDate nacimiento = fecha(fila.valor("fechaNacimiento"), errores);
        Genero genero = genero(fila.valor("genero"), errores);
        String codigo = recortar(fila.valor("codigo"), 20, "Código", errores);
        String direccion = recortar(fila.valor("direccion"), 200, "Dirección", errores);
        String telefono = recortar(fila.valor("telefono"), 20, "Teléfono", errores);
        String eps = recortar(fila.valor("eps"), 100, "EPS", errores);
        if (nombres != null && nombres.length() > 100) {
            errores.add("Los nombres tienen más de 100 caracteres");
        }
        if (apellidos != null && apellidos.length() > 100) {
            errores.add("Los apellidos tienen más de 100 caracteres");
        }

        Sede sede = catalogo.sede(fila.valor("sede"), errores);
        Grado grado = catalogo.grado(fila.valor("grado"), errores);
        Grupo grupo = null;
        String nombreGrupo = fila.valor("grupo");
        if (nombreGrupo != null && sede != null && grado != null) {
            grupo = catalogo.grupo(sede, grado, nombreGrupo);
            if (grupo == null) {
                avisos.add("No existe el grupo " + nombreGrupo + " de " + grado.getNombre() + " en " + sede.getNombre()
                        + "; queda matriculado sin grupo");
            }
        } else if (nombreGrupo == null) {
            avisos.add("Sin grupo: se puede asignar después en la ficha");
        }

        GuardarAcudienteDto acudiente = acudiente(fila, errores);

        GuardarEstudianteDto estudiante = errores.isEmpty()
                ? new GuardarEstudianteDto(tipo, documento, nombres, apellidos, telefono, null, codigo, nacimiento,
                        genero, direccion, eps, null, null, false, null)
                : null;
        return new Registro(fila.numero(), estudiante, sede, grado, grupo, acudiente, errores, avisos);
    }

    private static GuardarAcudienteDto acudiente(Fila fila, List<String> errores) {
        String documento = fila.valor("acudienteDocumento");
        if (documento == null) {
            return null;
        }
        List<String> propios = new ArrayList<>();
        String tipoTexto = fila.valor("acudienteTipoDocumento");
        TipoDocumento tipo = tipoTexto == null ? TipoDocumento.CC
                : tipoDocumento(tipoTexto, "Tipo de documento del acudiente", propios);
        String numero = documento(documento, "Documento del acudiente", propios);
        String nombres = fila.valor("acudienteNombres");
        String apellidos = fila.valor("acudienteApellidos");
        if (nombres == null || apellidos == null) {
            propios.add("Faltan nombres o apellidos del acudiente");
        }
        Parentesco parentesco = parentesco(fila.valor("parentesco"), propios);
        String telefono = recortar(fila.valor("acudienteTelefono"), 20, "Teléfono del acudiente", propios);
        if (fila.valor("numeroDocumento") != null && fila.valor("numeroDocumento").equals(numero)) {
            propios.add("El documento del acudiente es el mismo del estudiante");
        }
        errores.addAll(propios);
        return propios.isEmpty()
                ? new GuardarAcudienteDto(tipo, numero, nombres, apellidos, telefono, null, null, parentesco, false)
                : null;
    }

    /** Sedes, grados y grupos del anio, buscados por nombre sin importar tildes ni mayusculas. */
    private final class Catalogo {

        private final Map<String, Sede> sedes = new HashMap<>();
        private final Map<String, Grado> grados = new HashMap<>();
        private final Map<String, Grupo> grupos = new HashMap<>();
        private final int totalSedes;

        Catalogo(AnioLectivo anio) {
            List<Sede> lista = sedeService.listar();
            totalSedes = lista.size();
            for (Sede s : lista) {
                sedes.put(LectorExcel.normalizar(s.getNombre()), s);
                if (s.getCodigoDane() != null) {
                    sedes.put(LectorExcel.normalizar(s.getCodigoDane()), s);
                }
                if (lista.size() == 1) {
                    sedes.put("", s);
                }
            }
            for (Grado g : gradoService.listar()) {
                grados.put(LectorExcel.normalizar(g.getNombre()), g);
                grados.put(String.valueOf(g.getOrden()), g);
            }
            for (Grupo g : grupoRepository.listar(anio.getId(), null)) {
                grupos.put(g.getSede().getId() + "|" + g.getGrado().getId() + "|" + LectorExcel.normalizar(g.getNombre()), g);
            }
        }

        Sede sede(String texto, List<String> errores) {
            Sede sede = sedes.get(LectorExcel.normalizar(texto));
            if (sede == null) {
                errores.add(texto == null && totalSedes > 1 ? "Falta la sede" : "No existe la sede " + texto);
            }
            return sede;
        }

        Grado grado(String texto, List<String> errores) {
            if (texto == null) {
                errores.add("Falta el grado");
                return null;
            }
            String clave = LectorExcel.normalizar(texto).replace("grado", "");
            if (clave.matches("\\d+[o]?")) {
                clave = String.valueOf(Integer.parseInt(clave.replace("o", "")));
            }
            if (clave.equals("preescolar") || clave.equals("transicion")) {
                clave = "0";
            }
            Grado grado = grados.get(clave);
            if (grado == null) {
                errores.add("No existe el grado " + texto);
            }
            return grado;
        }

        Grupo grupo(Sede sede, Grado grado, String nombre) {
            return grupos.get(sede.getId() + "|" + grado.getId() + "|" + LectorExcel.normalizar(nombre));
        }
    }

    private static TipoDocumento tipoDocumento(String texto, String campo, List<String> errores) {
        if (texto == null) {
            errores.add("Falta el " + campo.toLowerCase());
            return null;
        }
        String clave = LectorExcel.normalizar(texto);
        TipoDocumento tipo = switch (clave) {
            case "rc", "registrocivil", "nuip" -> TipoDocumento.RC;
            case "ti", "tarjetadeidentidad", "tarjetaidentidad" -> TipoDocumento.TI;
            case "cc", "cedula", "ceduladeciudadania", "cedulaciudadania" -> TipoDocumento.CC;
            case "ce", "ceduladeextranjeria", "cedulaextranjeria" -> TipoDocumento.CE;
            case "ppt", "pep" -> TipoDocumento.PPT;
            default -> null;
        };
        if (tipo == null) {
            errores.add(campo + " no válido: " + texto + " (use RC, TI, CC, CE o PPT)");
        }
        return tipo;
    }

    private static String documento(String texto, String campo, List<String> errores) {
        if (texto == null) {
            errores.add("Falta el " + campo.toLowerCase());
            return null;
        }
        String limpio = texto.replaceAll("[.\\s-]", "");
        if (!limpio.matches("[0-9A-Za-z]{3,20}")) {
            errores.add(campo + " no válido: " + texto);
            return null;
        }
        return limpio;
    }

    private static LocalDate fecha(String texto, List<String> errores) {
        if (texto == null) {
            errores.add("Falta la fecha de nacimiento");
            return null;
        }
        for (DateTimeFormatter formato : FORMATOS_FECHA) {
            try {
                LocalDate fecha = LocalDate.parse(texto, formato);
                if (!fecha.isBefore(LocalDate.now()) || fecha.getYear() < 1900) {
                    errores.add("Fecha de nacimiento no válida: " + texto);
                    return null;
                }
                return fecha;
            } catch (DateTimeParseException e) {
                // Se prueba el siguiente formato
            }
        }
        errores.add("Fecha de nacimiento no válida: " + texto + " (use aaaa-mm-dd o dd/mm/aaaa)");
        return null;
    }

    private static Genero genero(String texto, List<String> errores) {
        String clave = LectorExcel.normalizar(texto);
        if (clave.equals("f") || clave.equals("femenino") || clave.equals("mujer")) {
            return Genero.FEMENINO;
        }
        if (clave.equals("m") || clave.equals("masculino") || clave.equals("hombre")) {
            return Genero.MASCULINO;
        }
        errores.add(texto == null ? "Falta el género" : "Género no válido: " + texto + " (use F o M)");
        return null;
    }

    private static Parentesco parentesco(String texto, List<String> errores) {
        if (texto == null) {
            return Parentesco.OTRO;
        }
        String clave = LectorExcel.normalizar(texto);
        Parentesco parentesco = switch (clave) {
            case "madre", "mama" -> Parentesco.MADRE;
            case "padre", "papa" -> Parentesco.PADRE;
            case "abuelo", "abuela", "abueloa" -> Parentesco.ABUELO;
            case "tio", "tia", "tioa" -> Parentesco.TIO;
            case "hermano", "hermana", "hermanoa" -> Parentesco.HERMANO;
            case "tutor", "tutora", "tutorlegal" -> Parentesco.TUTOR;
            case "otro", "otra" -> Parentesco.OTRO;
            default -> null;
        };
        if (parentesco == null) {
            errores.add("Parentesco no válido: " + texto);
        }
        return parentesco;
    }

    private static String unir(String completo, String primero, String segundo) {
        if (completo != null) {
            return completo;
        }
        String unido = ((primero == null ? "" : primero) + " " + (segundo == null ? "" : segundo)).trim();
        return unido.isEmpty() ? null : unido;
    }

    private static String recortar(String valor, int maximo, String campo, List<String> errores) {
        if (valor != null && valor.length() > maximo) {
            errores.add(campo + " tiene más de " + maximo + " caracteres");
        }
        return valor;
    }
}
