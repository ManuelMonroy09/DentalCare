package mx.dentalcare.domain.financiero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Cargo implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pacienteId;
    private Long citaId;
    private LocalDateTime fecha;
    private String concepto;
    private BigDecimal importe;

    public Cargo() {
    }

    public Cargo(Long pacienteId, Long citaId, LocalDateTime fecha, String concepto, BigDecimal importe) {
        this.pacienteId = pacienteId;
        this.citaId = citaId;
        this.fecha = fecha;
        this.concepto = concepto;
        this.importe = importe;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getCitaId() { return citaId; }
    public void setCitaId(Long citaId) { this.citaId = citaId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public String getConcepto() { return concepto; }
    public void setConcepto(String concepto) { this.concepto = concepto; }
    public BigDecimal getImporte() { return importe; }
    public void setImporte(BigDecimal importe) { this.importe = importe; }

    public void validar() {
        if (pacienteId == null) throw new IllegalStateException("El cargo debe tener un paciente.");
        if (citaId == null) throw new IllegalStateException("El cargo debe estar asociado a una cita.");
        if (fecha == null) throw new IllegalStateException("El cargo debe tener fecha.");
        if (concepto == null || concepto.isBlank()) throw new IllegalStateException("El cargo debe tener concepto.");
        if (importe == null || importe.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("El importe del cargo debe ser mayor a 0.");
    }
}
