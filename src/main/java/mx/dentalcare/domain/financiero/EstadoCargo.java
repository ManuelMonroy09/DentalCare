package mx.dentalcare.domain.financiero;

public enum EstadoCargo {
    PENDIENTE("Pendiente"),
    PARCIAL("Parcial"),
    PAGADO("Pagado");

    private final String descripcion;

    EstadoCargo(String descripcion) {
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
