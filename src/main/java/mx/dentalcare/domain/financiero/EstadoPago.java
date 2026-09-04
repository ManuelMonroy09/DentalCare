package mx.dentalcare.domain.financiero;

public enum EstadoPago {
    REGISTRADO("Registrado"),
    CANCELADO("Cancelado");

    private final String descripcion;

    EstadoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
