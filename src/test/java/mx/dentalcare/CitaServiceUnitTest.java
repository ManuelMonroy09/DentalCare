package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.event.CitaEstadoCambiadoEvent;
import mx.dentalcare.repository.CargoRepository;
import mx.dentalcare.repository.CitaRepository;
import mx.dentalcare.repository.PagoRepository;
import mx.dentalcare.service.AuditService;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.TratamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class CitaServiceUnitTest {

    private CitaRepository citaRepository;
    private TratamientoService tratamientoService;
    private CargoRepository cargoRepository;
    private PagoRepository pagoRepository;
    private ApplicationEventPublisher eventPublisher;
    private AuditService auditService;
    private CitaService service;

    @BeforeEach
    void setUp() {
        citaRepository = mock(CitaRepository.class);
        tratamientoService = mock(TratamientoService.class);
        cargoRepository = mock(CargoRepository.class);
        pagoRepository = mock(PagoRepository.class);
        eventPublisher = mock(ApplicationEventPublisher.class);
        auditService = mock(AuditService.class);
        service = new CitaService(citaRepository, tratamientoService, cargoRepository, pagoRepository, eventPublisher, auditService);
    }

    @Test
    void debeCrearCitaConMotivoYNotasNormalizados() {
        when(citaRepository.findAll()).thenReturn(List.of());
        Cita cita = service.crear(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0), 45, "  Consulta  ", "  Nota  ");

        assertEquals("Consulta", cita.getMotivo());
        assertEquals("Nota", cita.getNotas());
        assertEquals(45, cita.getDuracionMinutos());
        assertEquals(EstadoCita.PROGRAMADA, cita.getEstado());
    }

    @Test
    void debeRechazarDuracionNoPositiva() {
        when(citaRepository.findAll()).thenReturn(List.of());
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0), 0));
    }

    @Test
    void debeRechazarPacienteSinId() {
        Paciente paciente = paciente();
        paciente.setId(null);
        assertThrows(IllegalArgumentException.class,
                () -> service.crear(paciente, LocalDateTime.of(2030, 1, 1, 9, 0)));
    }

    @Test
    void debeCambiarInicioConservandoDuracion() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.findAll()).thenReturn(List.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        LocalDateTime nuevoInicio = LocalDateTime.of(2030, 1, 2, 11, 0);
        Cita resultado = service.cambiarInicio(1L, nuevoInicio);

        assertSame(cita, resultado);
        assertEquals(nuevoInicio, cita.getInicio());
        assertEquals(nuevoInicio.plusMinutes(60), cita.getFin());
    }

    @Test
    void debeCambiarDuracion() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.findAll()).thenReturn(List.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        service.cambiarDuracion(1L, 90);

        assertEquals(90, cita.getDuracionMinutos());
    }

    @Test
    void debeAgregarTratamientoActivo() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        Tratamiento tratamiento = tratamiento(8L, true);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(cargoRepository.findByCitaId(1L)).thenReturn(Optional.empty());
        when(pagoRepository.findAll()).thenReturn(List.of());
        when(tratamientoService.obtenerPorId(8L)).thenReturn(tratamiento);
        when(citaRepository.save(cita)).thenReturn(cita);

        service.agregarTratamiento(1L, 8L);

        assertEquals(1, cita.getTratamientos().size());
        assertEquals(8L, cita.getTratamientos().get(0).getTratamientoId());
        assertEquals("Limpieza", cita.getTratamientos().get(0).getNombre());
    }

    @Test
    void noDebeAgregarTratamientoInactivo() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(cargoRepository.findByCitaId(1L)).thenReturn(Optional.empty());
        when(pagoRepository.findAll()).thenReturn(List.of());
        when(tratamientoService.obtenerPorId(8L)).thenReturn(tratamiento(8L, false));

        assertThrows(IllegalStateException.class, () -> service.agregarTratamiento(1L, 8L));
        verify(citaRepository, never()).save(any());
    }

    @Test
    void debeSincronizarTratamientosAlGuardar() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        Tratamiento nuevo = tratamiento(9L, true);
        cita.agregarTratamiento(new TratamientoAplicado(8L, "Limpieza", new BigDecimal("500"), 45));
        when(cargoRepository.findByCitaId(1L)).thenReturn(Optional.empty());
        when(pagoRepository.findAll()).thenReturn(List.of());
        when(tratamientoService.obtenerPorId(9L)).thenReturn(nuevo);
        when(citaRepository.save(cita)).thenReturn(cita);

        service.guardarConTratamientos(cita, List.of(9L));

        assertEquals(1, cita.getTratamientos().size());
        assertEquals(9L, cita.getTratamientos().get(0).getTratamientoId());
    }

    @Test
    void debeCambiarEstadosYPublicarEventosCuandoCorresponde() {
        Cita cita = cita(1L, EstadoCita.PROGRAMADA);
        when(citaRepository.findById(1L)).thenReturn(Optional.of(cita));
        when(citaRepository.save(cita)).thenReturn(cita);

        service.confirmar(1L);
        assertEquals(EstadoCita.CONFIRMADA, cita.getEstado());
        verify(eventPublisher).publishEvent(any(CitaEstadoCambiadoEvent.class));

        service.marcarAtendida(1L);
        assertEquals(EstadoCita.ATENDIDA, cita.getEstado());
        verify(eventPublisher, times(2)).publishEvent(any(CitaEstadoCambiadoEvent.class));

        service.marcarNoAsistio(1L);
        assertEquals(EstadoCita.NO_ASISTIO, cita.getEstado());
        verify(eventPublisher, times(2)).publishEvent(any(CitaEstadoCambiadoEvent.class));

        service.cancelar(1L);
        assertEquals(EstadoCita.CANCELADA, cita.getEstado());
        verify(eventPublisher, times(2)).publishEvent(any(CitaEstadoCambiadoEvent.class));
    }

    @Test
    void debeObtenerCitasPorFechaOrdenadas() {
        Cita temprano = cita(1L, EstadoCita.PROGRAMADA);
        temprano.setInicio(LocalDateTime.of(2030, 1, 5, 9, 0));
        temprano.setFin(LocalDateTime.of(2030, 1, 5, 10, 0));
        Cita tarde = cita(2L, EstadoCita.PROGRAMADA);
        tarde.setInicio(LocalDateTime.of(2030, 1, 5, 11, 0));
        tarde.setFin(LocalDateTime.of(2030, 1, 5, 12, 0));
        when(citaRepository.findAll()).thenReturn(List.of(tarde, temprano));

        List<Cita> resultado = service.obtenerPorFecha(LocalDate.of(2030, 1, 5));

        assertEquals(List.of(temprano, tarde), resultado);
    }

    @Test
    void debeFiltrarHistorialSoloConCitasAtendidas() {
        Cita atendida = cita(1L, EstadoCita.ATENDIDA);
        Cita programada = cita(2L, EstadoCita.PROGRAMADA);
        when(citaRepository.findAll()).thenReturn(List.of(programada, atendida));

        assertEquals(List.of(atendida), service.obtenerHistorial());
    }

    private Paciente paciente() {
        return new Paciente(3L, "Juan", "Perez", "Lopez", "5512345678", null);
    }

    private Cita cita(Long id, EstadoCita estado) {
        Cita cita = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0));
        cita.setId(id);
        cita.setEstado(estado);
        return cita;
    }

    private Tratamiento tratamiento(Long id, boolean activo) {
        Tratamiento t = new Tratamiento();
        t.setId(id);
        t.setNombre("Limpieza");
        t.setPrecio(new BigDecimal("500"));
        t.setDuracionMinutos(45);
        t.setActivo(activo);
        return t;
    }
}
