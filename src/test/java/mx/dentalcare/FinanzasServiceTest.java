package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.EstadoPago;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.financiero.TipoPago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.event.CitaEstadoCambiadoEvent;
import mx.dentalcare.repository.CargoRepository;
import mx.dentalcare.repository.PagoRepository;
import mx.dentalcare.service.AuditService;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.FinanzasService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class FinanzasServiceTest {

    private CargoRepository cargoRepository;
    private PagoRepository pagoRepository;
    private CitaService citaService;
    private AuditService auditService;
    private FinanzasService service;

    @BeforeEach
    void setUp() {
        cargoRepository = mock(CargoRepository.class);
        pagoRepository = mock(PagoRepository.class);
        citaService = mock(CitaService.class);
        auditService = mock(AuditService.class);
        service = new FinanzasService(cargoRepository, pagoRepository, citaService, auditService);
    }

    @Test
    void debeRegistrarPagoValido() {
        Cargo cargo = cargo(10L, 7L, 1000);
        when(cargoRepository.findById(10L)).thenReturn(Optional.of(cargo));
        when(pagoRepository.findAll()).thenReturn(List.of());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(20L);
            return p;
        });

        Pago pago = service.registrarPago(10L, new BigDecimal("250"), MetodoPago.EFECTIVO, "  consulta  ");

        assertEquals(20L, pago.getId());
        assertEquals(new BigDecimal("250.00"), pago.getMonto());
        assertEquals(MetodoPago.EFECTIVO, pago.getMetodoPago());
        assertEquals("consulta", pago.getNotas());
        assertEquals(EstadoPago.REGISTRADO, pago.getEstado());
        assertEquals(TipoPago.PAGO, pago.getTipo());
    }

    @Test
    void noDebePermitirPagoMayorAlSaldo() {
        Cargo cargo = cargo(10L, 7L, 1000);
        Pago existente = pago(10L, 100L, 500L, TipoPago.PAGO, EstadoPago.REGISTRADO);
        when(cargoRepository.findById(10L)).thenReturn(Optional.of(cargo));
        when(pagoRepository.findAll()).thenReturn(List.of(existente));

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarPago(10L, new BigDecimal("501"), MetodoPago.EFECTIVO, null));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void noDebePermitirPagoConMontoNoPositivo() {
        Cargo cargo = cargo(10L, 7L, 1000);
        when(cargoRepository.findById(10L)).thenReturn(Optional.of(cargo));
        when(pagoRepository.findAll()).thenReturn(List.of());

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarPago(10L, BigDecimal.ZERO, MetodoPago.EFECTIVO, null));
    }

    @Test
    void debeCalcularSaldoYEstadoDelCargo() {
        Cargo cargo = cargo(10L, 7L, 1000);
        Pago pago = pago(10L, 100L, 400L, TipoPago.PAGO, EstadoPago.REGISTRADO);
        when(cargoRepository.findById(10L)).thenReturn(Optional.of(cargo));
        when(pagoRepository.findAll()).thenReturn(List.of(pago));

        assertEquals(new BigDecimal("400.00"), service.obtenerTotalPagado(10L));
        assertEquals(new BigDecimal("600.00"), service.obtenerSaldoPendiente(10L));
        assertEquals(EstadoCargo.PARCIAL, service.obtenerEstadoCargo(10L));
    }

    @Test
    void pagoCanceladoNoDebeContarComoPagado() {
        Cargo cargo = cargo(10L, 7L, 1000);
        Pago cancelado = pago(10L, 100L, 400L, TipoPago.PAGO, EstadoPago.CANCELADO);
        when(cargoRepository.findById(10L)).thenReturn(Optional.of(cargo));
        when(pagoRepository.findAll()).thenReturn(List.of(cancelado));

        assertEquals(new BigDecimal("0.00"), service.obtenerTotalPagado(10L));
        assertEquals(new BigDecimal("1000.00"), service.obtenerSaldoPendiente(10L));
        assertEquals(EstadoCargo.PENDIENTE, service.obtenerEstadoCargo(10L));
    }

    @Test
    void debeRegistrarAnticipoParaCitaProgramada() {
        Cita cita = cita(7L, 3L, EstadoCita.PROGRAMADA, 500);
        when(citaService.obtenerPorId(7L)).thenReturn(Optional.of(cita));
        when(pagoRepository.findAll()).thenReturn(List.of());
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });

        Pago anticipo = service.registrarAnticipo(7L, new BigDecimal("200"), MetodoPago.TRANSFERENCIA, "  anticipo  ");

        assertEquals(30L, anticipo.getId());
        assertEquals(7L, anticipo.getCitaId());
        assertNull(anticipo.getCargoId());
        assertEquals(TipoPago.ANTICIPO, anticipo.getTipo());
        assertEquals("anticipo", anticipo.getNotas());
    }

    @Test
    void noDebeRegistrarAnticipoEnCitaAtendida() {
        Cita cita = cita(7L, 3L, EstadoCita.ATENDIDA, 500);
        when(citaService.obtenerPorId(7L)).thenReturn(Optional.of(cita));

        assertThrows(IllegalStateException.class,
                () -> service.registrarAnticipo(7L, new BigDecimal("100"), MetodoPago.EFECTIVO, null));
        verify(pagoRepository, never()).save(any());
    }

    @Test
    void noDebePermitirAnticipoMayorAlTotalDeTratamientos() {
        Cita cita = cita(7L, 3L, EstadoCita.CONFIRMADA, 500);
        Pago anticipoExistente = Pago.anticipo(3L, 7L, LocalDateTime.now(), new BigDecimal("400"), MetodoPago.EFECTIVO, null);
        when(citaService.obtenerPorId(7L)).thenReturn(Optional.of(cita));
        when(pagoRepository.findAll()).thenReturn(List.of(anticipoExistente));

        assertThrows(IllegalArgumentException.class,
                () -> service.registrarAnticipo(7L, new BigDecimal("101"), MetodoPago.EFECTIVO, null));
    }

    @Test
    void debeGenerarCargoParaCitaAtendidaConTratamientos() {
        Cita cita = cita(7L, 3L, EstadoCita.ATENDIDA, 800);
        when(cargoRepository.findByCitaId(7L)).thenReturn(Optional.empty());
        when(cargoRepository.save(any(Cargo.class))).thenAnswer(inv -> {
            Cargo c = inv.getArgument(0);
            c.setId(40L);
            return c;
        });
        when(pagoRepository.findAll()).thenReturn(List.of());

        Cargo cargo = service.obtenerOCrearCargo(cita);

        assertEquals(40L, cargo.getId());
        assertEquals(3L, cargo.getPacienteId());
        assertEquals(7L, cargo.getCitaId());
        assertEquals(new BigDecimal("800.00"), cargo.getImporte());
        assertTrue(cargo.getConcepto().contains("Consulta"));
    }

    @Test
    void noDebeGenerarCargoParaCitaNoAtendida() {
        Cita cita = cita(7L, 3L, EstadoCita.CONFIRMADA, 800);
        assertThrows(IllegalStateException.class, () -> service.obtenerOCrearCargo(cita));
        verify(cargoRepository, never()).save(any());
    }

    @Test
    void debeCancelarPagoActivo() {
        Pago pago = pago(10L, 100L, 250L, TipoPago.PAGO, EstadoPago.REGISTRADO);
        pago.setId(20L);
        when(pagoRepository.findById(20L)).thenReturn(Optional.of(pago));

        service.cancelarPago(20L);

        assertEquals(EstadoPago.CANCELADO, pago.getEstado());
        verify(pagoRepository).save(pago);
        verify(auditService).registrar(eq("FINANZAS"), eq("CANCELAR_PAGO"), eq("PAGO"), eq(20L),
                anyString(), eq("ACTIVO"), eq("CANCELADO"), eq("EXITOSO"));
    }

    @Test
    void debeCalcularIngresosPorRangoDeFechas() {
        Pago p1 = pago(1L, 100L, 100, TipoPago.PAGO, EstadoPago.REGISTRADO);
        p1.setFecha(LocalDateTime.of(2030, 1, 10, 10, 0));
        Pago p2 = pago(1L, 101L, 250, TipoPago.PAGO, EstadoPago.REGISTRADO);
        p2.setFecha(LocalDateTime.of(2030, 1, 20, 10, 0));
        when(pagoRepository.findAll()).thenReturn(List.of(p1, p2));

        assertEquals(new BigDecimal("350.00"), service.obtenerIngresos(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 31)));
        assertEquals(new BigDecimal("100.00"), service.obtenerIngresos(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 15)));
    }

    private Cargo cargo(Long id, Long citaId, int importe) {
        Cargo cargo = new Cargo(1L, citaId, LocalDateTime.of(2030, 1, 1, 10, 0), "Consulta", new BigDecimal(importe));
        cargo.setId(id);
        return cargo;
    }

    private Pago pago(Long cargoId, Long id, int monto, TipoPago tipo, EstadoPago estado) {
        Pago pago = new Pago(1L, cargoId, LocalDateTime.of(2030, 1, 1, 10, 0), new BigDecimal(monto), MetodoPago.EFECTIVO, null);
        pago.setId(id);
        pago.setTipo(tipo);
        pago.setEstado(estado);
        return pago;
    }

    private Cita cita(Long id, Long pacienteId, EstadoCita estado, int precio) {
        Paciente paciente = new Paciente(pacienteId, "Juan", "Perez", "Lopez", "5512345678", null);
        Cita cita = new Cita(paciente, LocalDateTime.of(2030, 1, 1, 10, 0));
        cita.setId(id);
        cita.setEstado(estado);
        cita.agregarTratamiento(new TratamientoAplicado(1L, "Consulta", new BigDecimal(precio), 60));
        return cita;
    }
}
