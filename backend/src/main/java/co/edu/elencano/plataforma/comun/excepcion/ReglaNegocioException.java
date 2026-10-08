package co.edu.elencano.plataforma.comun.excepcion;

/** Se lanza cuando una operacion viola una regla del negocio (por ejemplo, un dato duplicado). La API responde 409. */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
