package mx.dentalcare.domain.cita;

import mx.dentalcare.domain.paciente.Paciente;
import java.io.Serializable;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonIgnore;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import java.util.ArrayList;
import java.util.List;

public class Cita implements Serializable {

    private static final long serialVersionUID = 1L;
    public static final long DURACION_POR_DEFECTO_MINUTOS = 60;
    private Long id;
    private Paciente paciente;
    private LocalDateTime inicio;
    private LocalDateTime fin;
    private String motivo;
    private EstadoCita estado;
    private String notas;
    private List<TratamientoAplicado> tratamientos;

    public Cita() {
        this.estado = EstadoCita.PROGRAMADA;
        this.tratamientos = new ArrayList<>();
    }

    public Cita(Paciente paciente, LocalDateTime inicio) {
        this(paciente, inicio, inicio != null ? inicio.plusMinutes(DURACION_POR_DEFECTO_MINUTOS) : null);
    }

    public Cita(Paciente paciente, LocalDateTime inicio, LocalDateTime fin) {
        this.paciente = paciente;
        this.inicio = inicio;
        this.fin = fin;
        this.estado = EstadoCita.PROGRAMADA;
        this.tratamientos = new ArrayList<>();
    }

    public Long getId() {
        return id;
    }
    public void setId(Long id) {
        this.id = id;
    }
    public Paciente getPaciente() {
        return paciente;
    }
    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }
    public LocalDateTime getInicio() {
        return inicio;
    }
    public void setInicio(LocalDateTime inicio) {
        this.inicio = inicio;
    }
    public LocalDateTime getFin() {
        return fin;
    }
    public void setFin(LocalDateTime fin) {
        this.fin = fin;
    }
    public String getMotivo() {
        return motivo;
    }
    public void setMotivo(String motivo) {
        this.motivo = motivo;
    }
    public EstadoCita getEstado() {
        return estado;
    }
    public void setEstado(EstadoCita estado) {
        this.estado = estado;
    }
    public String getNotas() {
        return notas;
    }
    public void setNotas(String notas) {
        this.notas = notas;
    }

    @JsonIgnore
    public long getDuracionMinutos() {
        if (inicio == null || fin == null) {
            return 0;
        }
        return Duration.between(inicio, fin).toMinutes();
    }

    public void establecerDuracion(long minutos) {
        if (minutos <= 0) {
            throw new IllegalArgumentException("La duración de la cita debe ser mayor a 0 minutos.");
        }
        if (inicio == null) {
            throw new IllegalStateException("No se puede establecer la duración sin una fecha de inicio.");
        }
        this.fin = inicio.plusMinutes(minutos);
    }

    public boolean tieneHorarioValido() {
        return inicio != null && fin != null && fin.isAfter(inicio);
    }

    public void validar() {
        if (paciente == null) {throw new IllegalStateException("La cita debe estar asociada a un paciente.");
        }

        if (inicio == null) {
            throw new IllegalStateException("La cita debe tener una fecha y hora de inicio.");
        }

        if (fin == null) {
            throw new IllegalStateException("La cita debe tener una fecha y hora de finalización.");
        }

        if (!fin.isAfter(inicio)) {
            throw new IllegalStateException("La fecha y hora de finalización debe ser posterior al inicio.");
        }

        if (estado == null) {throw new IllegalStateException("La cita debe tener un estado.");
        }
    }

    @JsonIgnore
    public String getNombrePaciente() {

        if (paciente == null) {
            return "";
        }
        String nombre = paciente.getNombre() != null ? paciente.getNombre() : "";
        String apellidoPaterno = paciente.getApellidoPaterno() != null ? paciente.getApellidoPaterno() : "";
        String apellidoMaterno = paciente.getApellidoMaterno() != null ? paciente.getApellidoMaterno() : "";
        return (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim().replaceAll("\\s+", " ");
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) {
            return true;
        }

        if (!(o instanceof Cita)) {
            return false;
        }

        Cita cita = (Cita) o;

        if (id == null || cita.id == null) {
            return false;
        }

        return Objects.equals(id, cita.id);
    }

    public List<TratamientoAplicado> getTratamientos() {
        return tratamientos;
    }

    public void setTratamientos(List<TratamientoAplicado> tratamientos) {
        this.tratamientos = tratamientos != null ? tratamientos : new ArrayList<>();
    }

    public void agregarTratamiento(TratamientoAplicado tratamiento) {
        if (tratamiento == null) {
            throw new IllegalArgumentException("El tratamiento aplicado no puede ser nulo.");
        }
        tratamiento.validar();
        if (tratamientos == null) {
            tratamientos = new ArrayList<>();
        }
        tratamientos.add(tratamiento);
    }

    public void quitarTratamiento(TratamientoAplicado tratamiento) {
        if (tratamientos == null || tratamiento == null) {
            return;
        }
        tratamientos.remove(tratamiento);
    }

    public BigDecimal obtenerTotalTratamientos() {
        if (tratamientos == null || tratamientos.isEmpty()) {
            return BigDecimal.ZERO;
        }
        return tratamientos.stream().map(TratamientoAplicado::obtenerImporte).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : System.identityHashCode(this);
    }

    @Override
    public String toString() {

        return "Cita{" + "id=" + id
                + ", paciente=" + getNombrePaciente()
                + ", inicio=" + inicio
                + ", fin=" + fin
                + ", motivo='" + motivo + '\''
                + ", estado=" + estado
                + ", notas='" + notas + '\''
                + '}';
    }

}