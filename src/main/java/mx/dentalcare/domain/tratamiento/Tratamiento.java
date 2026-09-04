package mx.dentalcare.domain.tratamiento;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Objects;

public class Tratamiento implements Serializable {

    private static final long serialVersionUID = 1L;
    private Long id;
    private String nombre;
    private String descripcion;
    private BigDecimal precio;
    private int duracionMinutos;
    private boolean activo;

    public Tratamiento() {
        this.activo = true;
        this.precio = BigDecimal.ZERO;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = normalizar(nombre);
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = normalizar(descripcion);
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

    public boolean isActivo() {
        return activo;
    }

    public void setActivo(boolean activo) {
        this.activo = activo;
    }

    public void validar() {

        if (nombre == null || nombre.isBlank()) {
            throw new IllegalStateException("El nombre del tratamiento es obligatorio.");
        }

        if (precio == null) {
            throw new IllegalStateException("El precio del tratamiento es obligatorio.");
        }

        if (precio.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalStateException("El precio no puede ser negativo.");
        }

        if (duracionMinutos <= 0) {
            throw new IllegalStateException("La duración debe ser mayor a 0 minutos.");
        }
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

        if (!(o instanceof Tratamiento)) {
            return false;
        }

        Tratamiento that = (Tratamiento) o;

        if (id == null || that.id == null) {
            return false;
        }

        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {

        return "Tratamiento{" +
                "id=" + id +
                ", nombre='" + nombre + '\'' +
                ", precio=" + precio +
                ", duracionMinutos=" + duracionMinutos +
                ", activo=" + activo +
                '}';
    }

    public void desactivar() {
        this.activo = false;
    }

    public void activar() {
        this.activo = true;
    }
}