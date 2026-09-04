package mx.dentalcare.service;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.EstadoPago;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.repository.CargoRepository;
import mx.dentalcare.repository.PagoRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class FinanzasService {
    private final CargoRepository cargoRepository;
    private final PagoRepository pagoRepository;
    private final CitaService citaService;

    public FinanzasService(CargoRepository cargoRepository, PagoRepository pagoRepository, CitaService citaService) {
        this.cargoRepository = cargoRepository;
        this.pagoRepository = pagoRepository;
        this.citaService = citaService;
    }

    public List<Cargo> obtenerCargos() {
        return cargoRepository.findAll().stream()
                .sorted(Comparator.comparing(Cargo::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public List<Pago> obtenerPagos() {
        return pagoRepository.findAll().stream()
                .filter(p -> p.getEstado() == null || p.getEstado().estaActivo())
                .sorted(Comparator.comparing(Pago::getFecha, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    public Cargo obtenerOCrearCargo(Cita cita) {
        if (cita == null || cita.getId() == null) {
            throw new IllegalArgumentException("La cita debe estar guardada.");
        }
        if (cita.getPaciente() == null || cita.getPaciente().getId() == null) {
            throw new IllegalArgumentException("La cita debe tener un paciente válido.");
        }
        if (cita.getEstado() != EstadoCita.ATENDIDA) {
            throw new IllegalStateException("Solo una cita atendida puede generar un cargo.");
        }

        return cargoRepository.findByCitaId(cita.getId()).orElseGet(() -> {
            BigDecimal total = dinero(cita.obtenerTotalTratamientos());
            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                throw new IllegalStateException("La cita no tiene tratamientos con importe para generar un cargo.");
            }

            Cargo cargo = new Cargo(
                    cita.getPaciente().getId(),
                    cita.getId(),
                    cita.getInicio() != null ? cita.getInicio() : LocalDateTime.now(),
                    construirConcepto(cita),
                    total
            );
            cargo.validar();
            return cargoRepository.save(cargo);
        });
    }

    public Pago registrarPago(Long cargoId, BigDecimal monto, MetodoPago metodoPago, String notas) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cargo seleccionado."));
        BigDecimal importe = dinero(monto);
        BigDecimal pendiente = obtenerSaldoPendiente(cargoId);

        if (importe.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        }
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("El cargo seleccionado ya está pagado.");
        }
        if (importe.compareTo(pendiente) > 0) {
            throw new IllegalArgumentException("El pago no puede superar el saldo pendiente de $" + pendiente.toPlainString());
        }

        Pago pago = new Pago(
                cargo.getPacienteId(),
                cargoId,
                LocalDateTime.now(),
                importe,
                metodoPago,
                notas == null ? null : notas.trim()
        );
        pago.validar();
        return pagoRepository.save(pago);
    }

    public void cancelarPago(Long pagoId) {
        if (pagoId == null) {
            throw new IllegalArgumentException("El identificador del pago es obligatorio.");
        }

        Pago pago = pagoRepository.findAll().stream()
                .filter(p -> pagoId.equals(p.getId()))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No existe el pago seleccionado."));

        if (pago.getEstado() == EstadoPago.CANCELADO) {
            throw new IllegalStateException("El pago seleccionado ya está cancelado.");
        }

        pago.setEstado(EstadoPago.CANCELADO);
        pagoRepository.save(pago);
    }

    public BigDecimal obtenerTotalPagado(Long cargoId) {
        return dinero(pagoRepository.findAll().stream()
                .filter(p -> cargoId != null && cargoId.equals(p.getCargoId()))
                .filter(p -> p.getEstado() == null || p.getEstado().estaActivo())
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal obtenerSaldoPendiente(Long cargoId) {
        Cargo cargo = cargoRepository.findById(cargoId)
                .orElseThrow(() -> new IllegalArgumentException("No existe el cargo seleccionado."));
        BigDecimal importe = dinero(cargo.getImporte());
        return dinero(importe.subtract(obtenerTotalPagado(cargoId)).max(BigDecimal.ZERO));
    }

    public EstadoCargo obtenerEstadoCargo(Long cargoId) {
        BigDecimal pagado = obtenerTotalPagado(cargoId);
        BigDecimal pendiente = obtenerSaldoPendiente(cargoId);

        if (pendiente.compareTo(BigDecimal.ZERO) == 0) {
            return EstadoCargo.PAGADO;
        }
        return pagado.compareTo(BigDecimal.ZERO) == 0
                ? EstadoCargo.PENDIENTE
                : EstadoCargo.PARCIAL;
    }

    public BigDecimal obtenerIngresos(LocalDate desde, LocalDate hasta) {
        return dinero(obtenerPagos().stream()
                .filter(p -> dentroDe(p.getFecha(), desde, hasta))
                .map(Pago::getMonto)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal obtenerPorCobrar() {
        return dinero(obtenerCargos().stream()
                .map(c -> c.getImporte().subtract(obtenerTotalPagado(c.getId())))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .max(BigDecimal.ZERO));
    }

    private String construirConcepto(Cita cita) {
        if (cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) {
            return "Servicios de consulta";
        }

        String concepto = cita.getTratamientos().stream()
                .filter(Objects::nonNull)
                .map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .collect(Collectors.joining(", "));

        return concepto.isBlank() ? "Servicios de consulta" : concepto;
    }

    private boolean dentroDe(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return false;
        LocalDate dia = fecha.toLocalDate();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }

    private BigDecimal dinero(BigDecimal valor) {
        return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP);
    }
}
