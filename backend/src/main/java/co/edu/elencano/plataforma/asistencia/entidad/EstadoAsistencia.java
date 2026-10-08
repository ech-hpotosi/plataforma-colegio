package co.edu.elencano.plataforma.asistencia.entidad;

/**
 * Estado de un estudiante en una clase. FALTA cuenta como inasistencia injustificada;
 * FALTA_JUSTIFICADA y PERMISO cuentan como inasistencia justificada; RETARDO no es inasistencia.
 */
public enum EstadoAsistencia {
    ASISTIO,
    FALTA,
    FALTA_JUSTIFICADA,
    RETARDO,
    PERMISO;

    public boolean esInasistencia() {
        return this == FALTA || this == FALTA_JUSTIFICADA || this == PERMISO;
    }
}
