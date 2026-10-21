package co.edu.elencano.plataforma.matricula.web.dto;

import java.util.List;

/**
 * Resultado de revisar o cargar un archivo de estudiantes. Si aplicado es falso no se guardo nada.
 * La accion de cada fila es CREAR, ACTUALIZAR o ERROR.
 */
public record ResultadoImportacionDto(int anio, int total, int creados, int actualizados, int conErrores,
                                      boolean aplicado, List<Fila> filas) {

    public record Fila(int fila, String documento, String estudiante, String grado, String grupo, String accion,
                       List<String> errores, List<String> avisos) {
    }
}
