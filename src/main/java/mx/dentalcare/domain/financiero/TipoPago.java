package mx.dentalcare.domain.financiero;

import java.io.Serializable;

public enum TipoPago implements Serializable {
    ANTICIPO("Anticipo"),
    PAGO("Pago");

    private final String descripcion;

    TipoPago(String descripcion) {
        this.descripcion = descripcion;
    }

    public String getDescripcion() {
        return descripcion;
    }
}
