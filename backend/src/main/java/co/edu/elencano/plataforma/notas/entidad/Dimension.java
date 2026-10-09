package co.edu.elencano.plataforma.notas.entidad;

/** Dimensiones de la formacion integral que se evaluan segun el SIEE (art. 4.2). */
public enum Dimension {
    SABER("Saber"), HACER("Hacer"), SER("Ser");

    private final String nombre;

    Dimension(String nombre) {
        this.nombre = nombre;
    }

    public String getNombre() {
        return nombre;
    }
}
