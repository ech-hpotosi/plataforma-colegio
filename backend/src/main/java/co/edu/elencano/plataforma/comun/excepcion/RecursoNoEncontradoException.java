package co.edu.elencano.plataforma.comun.excepcion;

/** Se lanza cuando se busca un registro que no existe. La API responde 404. */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
