package co.edu.elencano.plataforma.notas.servicio;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import co.edu.elencano.plataforma.notas.entidad.Desempeno;

/**
 * Lo que lleva el boletin de un grupo en un periodo segun el SIEE (art. 14), ya calculado.
 * El PDF solo lo dibuja.
 */
public record DatosBoletin(int anio, int periodo, LocalDate fecha, String sede, String grupo, String jornada,
                           String director, String coordinador, Escala escala, List<Estudiante> estudiantes) {

    /** Limites de la escala para explicar los desempenos al pie del boletin. */
    public record Escala(BigDecimal minima, BigDecimal maxima, BigDecimal aprobatoria, BigDecimal limiteAlto,
                         BigDecimal limiteSuperior) {
    }

    public record Estudiante(String nombre, String codigo, String documento, List<Asignatura> asignaturas,
                             BigDecimal comportamiento, Desempeno desempenoComportamiento, int valoracionesComportamiento) {
    }

    public record Asignatura(String area, String nombre, String docente, Integer intensidad, BigDecimal nota,
                             Desempeno desempeno, boolean completa, String concepto, String observacion,
                             long faltasJustificadas, long faltasSinJustificar) {
    }
}
