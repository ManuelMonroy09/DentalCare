package mx.dentalcare.domain.financiero;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public class Pago implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private Long pacienteId;
    private Long cargoId;
    private Long citaId;
    private LocalDateTime fecha;
    private BigDecimal monto;
    private MetodoPago metodoPago;
    private String notas;
    private EstadoPago estado;
    private TipoPago tipo;

    public Pago() {
        this.estado = EstadoPago.REGISTRADO;
        this.tipo = TipoPago.PAGO;
    }

    public Pago(Long pacienteId, Long cargoId, LocalDateTime fecha, BigDecimal monto, MetodoPago metodoPago, String notas) {
        this.pacienteId = pacienteId;
        this.cargoId = cargoId;
        this.fecha = fecha;
        this.monto = monto;
        this.metodoPago = metodoPago;
        this.notas = notas;
        this.estado = EstadoPago.REGISTRADO;
        this.tipo = TipoPago.PAGO;
    }

    public static Pago anticipo(Long pacienteId, Long citaId, LocalDateTime fecha, BigDecimal monto, MetodoPago metodoPago, String notas) {
        Pago pago = new Pago();
        pago.setPacienteId(pacienteId);
        pago.setCitaId(citaId);
        pago.setFecha(fecha);
        pago.setMonto(monto);
        pago.setMetodoPago(metodoPago);
        pago.setNotas(notas);
        pago.setTipo(TipoPago.ANTICIPO);
        return pago;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPacienteId() { return pacienteId; }
    public void setPacienteId(Long pacienteId) { this.pacienteId = pacienteId; }
    public Long getCargoId() { return cargoId; }
    public void setCargoId(Long cargoId) { this.cargoId = cargoId; }
    public Long getCitaId() { return citaId; }
    public void setCitaId(Long citaId) { this.citaId = citaId; }
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
    public TipoPago getTipo() { return tipo; }
    public void setTipo(TipoPago tipo) { this.tipo = tipo; }

    public void validar() {
        if (pacienteId == null) throw new IllegalStateException("El pago debe tener un paciente.");
        if (cargoId == null && citaId == null) throw new IllegalStateException("El pago debe estar asociado a un cargo o a una cita.");
        if (fecha == null) throw new IllegalStateException("El pago debe tener fecha.");
        if (monto == null || monto.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("El monto del pago debe ser mayor a 0.");
        if (metodoPago == null) throw new IllegalStateException("El método de pago es obligatorio.");
        if (estado == null) estado = EstadoPago.REGISTRADO;
        if (tipo == null) tipo = cargoId == null ? TipoPago.ANTICIPO : TipoPago.PAGO;
    }
}
