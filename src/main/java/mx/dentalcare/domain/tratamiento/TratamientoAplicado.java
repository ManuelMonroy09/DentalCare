package mx.dentalcare.domain.tratamiento;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class TratamientoAplicado implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long tratamientoId;
    private String nombre;
    private BigDecimal precio;
    private int duracionMinutos;

    public TratamientoAplicado() {
    }

    public TratamientoAplicado(Long tratamientoId, String nombre, BigDecimal precio, int duracionMinutos) {
        this.tratamientoId = tratamientoId;
        this.nombre = normalizar(nombre);
        this.precio = precio;
        this.duracionMinutos = duracionMinutos;
    }

    public Long getTratamientoId() {
        return tratamientoId;
    }

    public void setTratamientoId(Long tratamientoId) {
        this.tratamientoId = tratamientoId;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = normalizar(nombre);
    }

    public BigDecimal getPrecio() {
        return precio;
    }

    public void setPrecio(BigDecimal precio) {
        this.precio = precio;
    }

    public int getDuracionMinutos() {
        return duracionMinutos;
    }

    public void setDuracionMinutos(int duracionMinutos) {
        this.duracionMinutos = duracionMinutos;
    }

    public void validar() {

        if (tratamientoId == null) {
            throw new IllegalStateException("El tratamiento aplicado debe tener un ID.");
        }

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("El tratamiento aplicado debe tener un nombre.");
        }

        if (precio == null) {
            throw new IllegalStateException("El tratamiento aplicado debe tener un precio.");
        }

        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El precio del tratamiento aplicado no puede ser negativo.");
        }

        if (duracionMinutos <= 0) {
            throw new IllegalStateException("La duración del tratamiento aplicado debe ser mayor a 0 minutos.");
        }
    }

    public BigDecimal obtenerImporte() {
        return precio != null ? precio : BigDecimal.ZERO;
    }

    private String normalizar(String valor) {
        if (valor == null) {
            return null;
        }
        return valor.trim().replaceAll("\\s+", " ");
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof TratamientoAplicado)) {
            return false;
        }
        TratamientoAplicado that = (TratamientoAplicado) o;
        if (tratamientoId == null || that.tratamientoId == null) {
            return false;
        }

        return Objects.equals(tratamientoId, that.tratamientoId);
    }

    @Override
    public int hashCode() {
        return tratamientoId != null ? Objects.hash(tratamientoId) : System.identityHashCode(this);
    }

    @Override
    public String toString() {
        return "TratamientoAplicado{" + "tratamientoId=" + tratamientoId + ", nombre='" + nombre + '\'' + ", precio=" + precio + ", duracionMinutos=" + duracionMinutos + '}';
    }
}