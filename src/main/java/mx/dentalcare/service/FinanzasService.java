package mx.dentalcare.service;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.EstadoPago;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.financiero.TipoPago;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.event.CitaEstadoCambiadoEvent;
import mx.dentalcare.repository.CargoRepository;
import mx.dentalcare.repository.PagoRepository;
import org.springframework.context.event.EventListener;
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

    public List<Pago> obtenerAnticiposPorCita(Long citaId) {
        if (citaId == null) throw new IllegalArgumentException("El identificador de la cita es obligatorio.");
        return obtenerPagos().stream()
                .filter(p -> citaId.equals(p.getCitaId()) && p.getTipo() == TipoPago.ANTICIPO)
                .toList();
    }

    public BigDecimal obtenerTotalAnticipos(Long citaId) {
        return dinero(obtenerAnticiposPorCita(citaId).stream()
                .map(Pago::getMonto).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public List<Pago> obtenerPagosPorCargo(Long cargoId) {
        if (cargoId == null) throw new IllegalArgumentException("El identificador del cargo es obligatorio.");
        return obtenerPagos().stream().filter(p -> cargoId.equals(p.getCargoId()))
                .sorted(Comparator.comparing(Pago::getFecha, Comparator.nullsLast(Comparator.reverseOrder()))).toList();
    }

    public Pago obtenerPagoPorId(Long pagoId) {
        if (pagoId == null) throw new IllegalArgumentException("El identificador del pago es obligatorio.");
        return pagoRepository.findById(pagoId).orElseThrow(() -> new IllegalArgumentException("No existe el pago seleccionado."));
    }

    /** Genera cargos únicamente cuando la atención ya ocurrió. */
    public int generarCargosPendientes() {
        int creados = 0;
        for (Cita cita : citaService.obtenerTodas()) {
            if (cita.getEstado() != EstadoCita.ATENDIDA) continue;
            BigDecimal total = cita.obtenerTotalTratamientos();
            if (cita.getId() == null || total == null || total.compareTo(BigDecimal.ZERO) <= 0) continue;
            if (cargoRepository.findByCitaId(cita.getId()).isEmpty()) {
                obtenerOCrearCargo(cita);
                creados++;
            }
        }
        return creados;
    }

    public Cargo obtenerOCrearCargo(Cita cita) {
        if (cita == null || cita.getId() == null) throw new IllegalArgumentException("La cita debe estar guardada.");
        if (cita.getPaciente() == null || cita.getPaciente().getId() == null) throw new IllegalArgumentException("La cita debe tener un paciente válido.");
        if (cita.getEstado() != EstadoCita.ATENDIDA) throw new IllegalStateException("Solo una cita atendida puede generar un cargo por servicios realizados.");

        Cargo cargo = cargoRepository.findByCitaId(cita.getId()).orElseGet(() -> {
            BigDecimal total = dinero(cita.obtenerTotalTratamientos());
            if (total.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("La cita no tiene tratamientos con importe para generar un cargo.");
            Cargo nuevo = new Cargo(cita.getPaciente().getId(), cita.getId(),
                    cita.getInicio() != null ? cita.getInicio() : LocalDateTime.now(), construirConcepto(cita), total);
            nuevo.validar();
            return cargoRepository.save(nuevo);
        });

        vincularAnticiposAlCargo(cita.getId(), cargo.getId());
        return cargo;
    }

    private void vincularAnticiposAlCargo(Long citaId, Long cargoId) {
        for (Pago pago : pagoRepository.findAll()) {
            if (!citaId.equals(pago.getCitaId()) || pago.getTipo() != TipoPago.ANTICIPO) continue;
            if (pago.getCargoId() != null && pago.getCargoId().equals(cargoId)) continue;
            pago.setCargoId(cargoId);
            pago.setTipo(TipoPago.PAGO);
            pagoRepository.save(pago);
        }
    }

    @EventListener
    public void alCambiarEstadoCita(CitaEstadoCambiadoEvent event) {
        if (event == null || event.getCita() == null) return;
        Cita cita = event.getCita();
        if (cita.getEstado() != EstadoCita.ATENDIDA || cita.getId() == null) return;
        BigDecimal total = cita.obtenerTotalTratamientos();
        if (total == null || total.compareTo(BigDecimal.ZERO) <= 0) return;
        obtenerOCrearCargo(cita);
    }

    public Pago registrarPago(Long cargoId, BigDecimal monto, MetodoPago metodoPago, String notas) {
        if (cargoId == null) throw new IllegalArgumentException("El identificador del cargo es obligatorio.");
        if (metodoPago == null) throw new IllegalArgumentException("El método de pago es obligatorio.");
        Cargo cargo = cargoRepository.findById(cargoId).orElseThrow(() -> new IllegalArgumentException("No existe el cargo seleccionado."));
        BigDecimal importe = dinero(monto);
        BigDecimal pendiente = obtenerSaldoPendiente(cargoId);
        if (importe.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalStateException("El cargo seleccionado ya está pagado.");
        if (importe.compareTo(pendiente) > 0) throw new IllegalArgumentException("El pago no puede superar el saldo pendiente de $" + pendiente.toPlainString());
        Pago pago = new Pago(cargo.getPacienteId(), cargoId, LocalDateTime.now(), importe, metodoPago, notas == null ? null : notas.trim());
        pago.validar();
        return pagoRepository.save(pago);
    }

    public Pago registrarAnticipo(Long citaId, BigDecimal monto, MetodoPago metodoPago, String notas) {
        if (citaId == null) throw new IllegalArgumentException("El identificador de la cita es obligatorio.");
        if (metodoPago == null) throw new IllegalArgumentException("El método de pago es obligatorio.");
        Cita cita = citaService.obtenerPorId(citaId).orElseThrow(() -> new IllegalArgumentException("No existe la cita seleccionada."));
        if (cita.getEstado() != EstadoCita.PROGRAMADA && cita.getEstado() != EstadoCita.CONFIRMADA) {
            throw new IllegalStateException("Solo se puede registrar un anticipo para una cita programada o confirmada.");
        }
        if (cita.getPaciente() == null || cita.getPaciente().getId() == null) throw new IllegalStateException("La cita no tiene un paciente válido.");

        BigDecimal importe = dinero(monto);
        if (importe.compareTo(BigDecimal.ZERO) <= 0) throw new IllegalArgumentException("El monto debe ser mayor a 0.");
        BigDecimal totalTratamientos = dinero(cita.obtenerTotalTratamientos());
        BigDecimal anticiposActuales = obtenerTotalAnticipos(citaId);
        if (totalTratamientos.compareTo(BigDecimal.ZERO) > 0 && anticiposActuales.add(importe).compareTo(totalTratamientos) > 0) {
            throw new IllegalArgumentException("El anticipo no puede superar el importe actual de los tratamientos: $" + totalTratamientos.toPlainString());
        }

        Pago pago = Pago.anticipo(cita.getPaciente().getId(), citaId, LocalDateTime.now(), importe, metodoPago, notas == null ? null : notas.trim());
        pago.validar();
        return pagoRepository.save(pago);
    }

    public void cancelarPago(Long pagoId) {
        Pago pago = obtenerPagoPorId(pagoId);
        if (pago.getEstado() == EstadoPago.CANCELADO) throw new IllegalStateException("El pago seleccionado ya está cancelado.");
        pago.setEstado(EstadoPago.CANCELADO);
        pagoRepository.save(pago);
    }

    public BigDecimal obtenerTotalPagado(Long cargoId) {
        if (cargoId == null) return BigDecimal.ZERO.setScale(2);
        return dinero(pagoRepository.findAll().stream().filter(p -> cargoId.equals(p.getCargoId()))
                .filter(p -> p.getEstado() == null || p.getEstado().estaActivo()).map(Pago::getMonto).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public BigDecimal obtenerSaldoPendiente(Long cargoId) {
        if (cargoId == null) throw new IllegalArgumentException("El identificador del cargo es obligatorio.");
        Cargo cargo = cargoRepository.findById(cargoId).orElseThrow(() -> new IllegalArgumentException("No existe el cargo seleccionado."));
        return dinero(dinero(cargo.getImporte()).subtract(obtenerTotalPagado(cargoId)).max(BigDecimal.ZERO));
    }

    public EstadoCargo obtenerEstadoCargo(Long cargoId) {
        if (cargoId == null) throw new IllegalArgumentException("El identificador del cargo es obligatorio.");
        BigDecimal pagado = obtenerTotalPagado(cargoId);
        BigDecimal pendiente = obtenerSaldoPendiente(cargoId);
        if (pendiente.compareTo(BigDecimal.ZERO) == 0) return EstadoCargo.PAGADO;
        return pagado.compareTo(BigDecimal.ZERO) == 0 ? EstadoCargo.PENDIENTE : EstadoCargo.PARCIAL;
    }

    public BigDecimal obtenerIngresos(LocalDate desde, LocalDate hasta) {
        return dinero(obtenerPagos().stream().filter(p -> dentroDe(p.getFecha(), desde, hasta)).map(Pago::getMonto).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public int obtenerCantidadPagos(LocalDate desde, LocalDate hasta) {
        return (int) obtenerPagos().stream().filter(p -> dentroDe(p.getFecha(), desde, hasta)).count();
    }

    public BigDecimal obtenerPorCobrar() {
        return dinero(obtenerCargos().stream().map(c -> dinero(c.getImporte()).subtract(obtenerTotalPagado(c.getId())).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    public List<Cargo> obtenerCargosPorPaciente(Long pacienteId) {
        if (pacienteId == null) throw new IllegalArgumentException("El identificador del paciente es obligatorio.");
        return obtenerCargos().stream().filter(c -> pacienteId.equals(c.getPacienteId())).toList();
    }

    public BigDecimal obtenerSaldoPaciente(Long pacienteId) {
        return dinero(obtenerCargosPorPaciente(pacienteId).stream().map(c -> dinero(c.getImporte()).subtract(obtenerTotalPagado(c.getId())).max(BigDecimal.ZERO)).reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private String construirConcepto(Cita cita) {
        if (cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) return "Servicios de consulta";
        String concepto = cita.getTratamientos().stream().filter(Objects::nonNull).map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank()).collect(Collectors.joining(", "));
        return concepto.isBlank() ? "Servicios de consulta" : concepto;
    }

    private boolean dentroDe(LocalDateTime fecha, LocalDate desde, LocalDate hasta) {
        if (fecha == null) return false;
        LocalDate dia = fecha.toLocalDate();
        return (desde == null || !dia.isBefore(desde)) && (hasta == null || !dia.isAfter(hasta));
    }

    private BigDecimal dinero(BigDecimal valor) { return (valor == null ? BigDecimal.ZERO : valor).setScale(2, RoundingMode.HALF_UP); }
}
