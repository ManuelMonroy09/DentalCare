package mx.dentalcare.domain.financiero;

public enum EstadoPago {
    REGISTRADO("Registrado"),
    CANCELADO("Cancelado"),
    /** @deprecated Solo se conserva para compatibilidad con datos financieros anteriores. */
    @Deprecated PENDIENTE("Pendiente"),
    /** @deprecated Solo se conserva para compatibilidad con datos financieros anteriores. */
    @Deprecated PARCIAL("Parcial"),
    /** @deprecated Solo se conserva para compatibilidad con datos financieros anteriores. */
    @Deprecated PAGADO("Pagado");

    private final String descripcion;

    EstadoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public boolean estaActivo() {
        return this != CANCELADO;
    }

    @Override
    public String toString() {
        return descripcion;
    }
}
