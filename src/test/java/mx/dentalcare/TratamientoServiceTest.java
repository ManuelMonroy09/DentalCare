package mx.dentalcare;

import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.repository.TratamientoRepository;
import mx.dentalcare.service.AuditService;
import mx.dentalcare.service.TratamientoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class TratamientoServiceTest {

    private TratamientoRepository repository;
    private AuditService auditService;
    private TratamientoService service;

    @BeforeEach
    void setUp() {
        repository = mock(TratamientoRepository.class);
        auditService = mock(AuditService.class);
        service = new TratamientoService(repository, auditService);
    }

    @Test
    void debeCrearTratamientoValidoComoActivo() {
        Tratamiento tratamiento = tratamiento(null, "Limpieza", "500", 45, false);
        when(repository.save(tratamiento)).thenAnswer(inv -> {
            tratamiento.setId(10L);
            return tratamiento;
        });

        Tratamiento resultado = service.crear(tratamiento);

        assertEquals(10L, resultado.getId());
        assertTrue(resultado.isActivo());
        verify(repository).save(tratamiento);
        verify(auditService).registrar(eq("TRATAMIENTOS"), eq("CREAR"), eq("TRATAMIENTO"), eq(10L),
                anyString(), isNull(), eq("Tratamiento #10"), eq("EXITOSO"));
    }

    @Test
    void debeRechazarTratamientoNulo() {
        assertThrows(IllegalArgumentException.class, () -> service.crear(null));
        verifyNoInteractions(repository, auditService);
    }

    @Test
    void debeRechazarPrecioNegativo() {
        Tratamiento tratamiento = tratamiento(null, "Limpieza", "-1", 45, true);
        assertThrows(IllegalStateException.class, () -> service.crear(tratamiento));
        verify(repository, never()).save(any());
    }

    @Test
    void debeRechazarDuracionInvalida() {
        Tratamiento tratamiento = tratamiento(null, "Limpieza", "500", 0, true);
        assertThrows(IllegalStateException.class, () -> service.crear(tratamiento));
        verify(repository, never()).save(any());
    }

    @Test
    void debeActualizarTratamientoExistente() {
        Tratamiento tratamiento = tratamiento(5L, "Limpieza", "650", 60, true);
        when(repository.findById(5L)).thenReturn(Optional.of(tratamiento));
        when(repository.save(tratamiento)).thenReturn(tratamiento);

        Tratamiento resultado = service.actualizar(tratamiento);

        assertSame(tratamiento, resultado);
        verify(repository).save(tratamiento);
        verify(auditService).registrar(eq("TRATAMIENTOS"), eq("ACTUALIZAR"), eq("TRATAMIENTO"), eq(5L),
                anyString(), isNull(), eq("Tratamiento #5"), eq("EXITOSO"));
    }

    @Test
    void debeRechazarActualizarSinId() {
        Tratamiento tratamiento = tratamiento(null, "Limpieza", "500", 45, true);
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(tratamiento));
        verify(repository, never()).save(any());
    }

    @Test
    void debeRechazarActualizarTratamientoInexistente() {
        Tratamiento tratamiento = tratamiento(99L, "Limpieza", "500", 45, true);
        when(repository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.actualizar(tratamiento));
    }

    @Test
    void debeActivarYDesactivarTratamiento() {
        Tratamiento tratamiento = tratamiento(5L, "Limpieza", "500", 45, true);
        when(repository.findById(5L)).thenReturn(Optional.of(tratamiento));

        service.desactivar(5L);
        assertFalse(tratamiento.isActivo());
        service.activar(5L);
        assertTrue(tratamiento.isActivo());

        verify(repository, times(2)).save(tratamiento);
        verify(auditService).registrar(eq("TRATAMIENTOS"), eq("DESACTIVAR"), eq("TRATAMIENTO"), eq(5L),
                anyString(), eq("ACTIVO"), eq("INACTIVO"), eq("EXITOSO"));
        verify(auditService).registrar(eq("TRATAMIENTOS"), eq("ACTIVAR"), eq("TRATAMIENTO"), eq(5L),
                anyString(), eq("INACTIVO"), eq("ACTIVO"), eq("EXITOSO"));
    }

    @Test
    void debeFiltrarTratamientosActivos() {
        Tratamiento activo = tratamiento(1L, "Limpieza", "500", 45, true);
        Tratamiento inactivo = tratamiento(2L, "Extraccion", "800", 60, false);
        when(repository.findAll()).thenReturn(List.of(activo, inactivo));

        assertEquals(List.of(activo), service.obtenerActivos());
    }

    @Test
    void debeRechazarIdNuloAlConsultar() {
        assertThrows(IllegalArgumentException.class, () -> service.obtenerPorId(null));
    }

    private Tratamiento tratamiento(Long id, String nombre, String precio, int duracion, boolean activo) {
        Tratamiento t = new Tratamiento();
        t.setId(id);
        t.setNombre(nombre);
        t.setPrecio(new BigDecimal(precio));
        t.setDuracionMinutos(duracion);
        t.setActivo(activo);
        return t;
    }
}
