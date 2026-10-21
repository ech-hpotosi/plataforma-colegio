package co.edu.elencano.plataforma.matricula.entidad;

/**
 * Ciclo de una solicitud de matricula en linea. El acudiente solo la modifica en BORRADOR o
 * CON_OBSERVACIONES; la secretaria revisa las ENVIADA y las aprueba, devuelve o rechaza.
 */
public enum EstadoSolicitud {
    BORRADOR,
    ENVIADA,
    CON_OBSERVACIONES,
    APROBADA,
    RECHAZADA;

    public boolean editablePorAcudiente() {
        return this == BORRADOR || this == CON_OBSERVACIONES;
    }
}
