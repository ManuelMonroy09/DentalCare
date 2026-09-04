package mx.dentalcare.domain.financiero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pacienteId;
    private Long cargoId;
    private LocalDateTime fecha;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String notas;
    private EstadoPago estado;

    public Pago() {
        this.estado = EstadoPago.PAGADO;
    }

    public Pago(Long pacienteId, Long cargoId, LocalDateTime fecha, BigDecimal monto, MetodoPago metodoPago, String notas) {
        this.pacienteId = pacienteId;
        this.cargoId = cargoId;
        this.fecha = fecha;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.notas = notas;
        this.estado = EstadoPago.PAGADO;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getCargoId() { return cargoId; }
    public void setCargoId(Long cargoId) { this.cargoId = cargoId; }
    public LocalDateTime getFecha() { return fecha; }
    public void setFecha(LocalDateTime fecha) { this.fecha = fecha; }
    public BigDecimal getMonto() { return monto; }
    public void setMonto(BigDecimal monto) { this.monto = monto; }
    public MetodoPago getMetodoPago() { return metodoPago; }
    public void setMetodoPago(MetodoPago metodoPago) { this.metodoPago = metodoPago; }
    public String getNotas() { return notas; }
    public void setNotas(String notas) { this.notas = notas; }
    public EstadoPago getEstado() { return estado; }
    public void setEstado(EstadoPago estado) { this.estado = estado; }

    public void validar() {
        if (pacienteId == null) throw new IllegalStateException("El pago debe tener un paciente.");
        if (cargoId == null) throw new IllegalStateException("El pago debe estar asociado a un cargo.");
        if (fecha == null) throw new IllegalStateException("El pago debe tener fecha.");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("El monto del pago debe ser mayor a 0.");
        if (metodoPago == null) throw new IllegalStateException("El método de pago es obligatorio.");
        if (estado == null) estado = EstadoPago.PAGADO;
    }
}
