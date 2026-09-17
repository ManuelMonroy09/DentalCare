package mx.dentalcare;

import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.paciente.exception.PacienteValidationException;
import mx.dentalcare.repository.CitaRepository;
import mx.dentalcare.repository.PacienteRepository;
import mx.dentalcare.service.AuditService;
import mx.dentalcare.service.PacientesService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PacientesServiceTest {

    private PacienteRepository pacienteRepository;
    private CitaRepository citaRepository;
    private AuditService auditService;
    private PacientesService service;

    @BeforeEach
    void setUp() {
        pacienteRepository = mock(PacienteRepository.class);
        citaRepository = mock(CitaRepository.class);
        auditService = mock(AuditService.class);
        service = new PacientesService(pacienteRepository, citaRepository, auditService);
    }

    @Test
    void debeGuardarPacienteValidoYAuditarlo() {
        Paciente paciente = paciente("Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
        when(pacienteRepository.findAll()).thenReturn(List.of());
        when(pacienteRepository.save(paciente)).thenAnswer(inv -> {
            paciente.setId(10L);
            return paciente;
        });

        Paciente resultado = service.guardar(paciente);

        assertEquals(10L, resultado.getId());
        verify(pacienteRepository).save(paciente);
        verify(auditService).registrar(eq("PACIENTES"), eq("CREAR"), eq("PACIENTE"), eq(10L),
                anyString(), isNull(), eq("Paciente #10"), eq("EXITOSO"));
    }

    @Test
    void debeActualizarPacienteExistenteYAuditarlo() {
        Paciente paciente = paciente(10L, "Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
        when(pacienteRepository.findAll()).thenReturn(List.of(paciente));
        when(pacienteRepository.save(paciente)).thenReturn(paciente);

        assertSame(paciente, service.guardar(paciente));
        verify(auditService).registrar(eq("PACIENTES"), eq("ACTUALIZAR"), eq("PACIENTE"), eq(10L),
                anyString(), isNull(), eq("Paciente #10"), eq("EXITOSO"));
    }

    @Test
    void debeObtenerTodosYPorId() {
        Paciente p = paciente(1L, "Juan", "Perez", "Lopez", "5512345678", null);
        when(pacienteRepository.findAll()).thenReturn(List.of(p));
        when(pacienteRepository.findById(1L)).thenReturn(Optional.of(p));

        assertEquals(List.of(p), service.obtenerTodos());
        assertEquals(Optional.of(p), service.obtenerPorId(1L));
        verify(pacienteRepository).findAll();
        verify(pacienteRepository).findById(1L);
    }

    @Test
    void debeRechazarPacienteNulo() {
        assertThrows(PacienteValidationException.class, () -> service.guardar(null));
        verifyNoInteractions(pacienteRepository, auditService);
    }

    @Test
    void debeValidarCamposObligatorios() {
        Paciente paciente = new Paciente();
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente));
        verifyNoInteractions(pacienteRepository, auditService);
    }

    @Test
    void debeRechazarNombreInvalidoPorLongitudYFormato() {
        Paciente corto = paciente("J", "Perez", "Lopez", "5512345678", null);
        assertThrows(PacienteValidationException.class, () -> service.guardar(corto));

        Paciente largo = paciente("A".repeat(51), "Perez", "Lopez", "5512345678", null);
        assertThrows(PacienteValidationException.class, () -> service.guardar(largo));

        Paciente caracteres = paciente("Juan123", "Perez", "Lopez", "5512345678", null);
        assertThrows(PacienteValidationException.class, () -> service.guardar(caracteres));
    }

    @Test
    void debeAceptarNombreConApostrofeGuionYEspacios() {
        Paciente paciente = paciente("Jean-Paul D'Angelo", "Pérez-García", "Lopez", "5512345678", null);
        when(pacienteRepository.findAll()).thenReturn(List.of());
        when(pacienteRepository.save(any())).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        assertDoesNotThrow(() -> service.guardar(paciente));
    }

    @Test
    void debeRechazarApellidosInvalidos() {
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "P", "Lopez", "5512345678", null)));
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "Perez1", "Lopez", "5512345678", null)));
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "Perez", "L", "5512345678", null)));
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "Perez", "Lopez1", "5512345678", null)));
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "A".repeat(51), "Lopez", "5512345678", null)));
        assertThrows(PacienteValidationException.class,
                () -> service.guardar(paciente("Juan", "Perez", "A".repeat(51), "5512345678", null)));
    }

    @Test
    void debeRechazarTelefonoQueNoSeaDeDiezDigitos() {
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente("Juan", "Perez", "Lopez", "551234", null)));
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente("Juan", "Perez", "Lopez", "55123456789A", null)));
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente("Juan", "Perez", "Lopez", "A".repeat(11), null)));
    }

    @Test
    void debeAceptarEmailOpcionalYRechazarEmailInvalido() {
        Paciente sinEmail = paciente("Juan", "Perez", "Lopez", "5512345678", null);
        when(pacienteRepository.findAll()).thenReturn(List.of());
        when(pacienteRepository.save(any())).thenAnswer(inv -> {
            Paciente p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });
        assertDoesNotThrow(() -> service.guardar(sinEmail));

        Paciente emailInvalido = paciente("Ana", "Perez", "Lopez", "5512345679", "correo-invalido");
        assertThrows(PacienteValidationException.class, () -> service.guardar(emailInvalido));
        Paciente emailLargo = paciente("Ana", "Perez", "Lopez", "5512345680", "a".repeat(101) + "@test.com");
        assertThrows(PacienteValidationException.class, () -> service.guardar(emailLargo));
    }

    @Test
    void debeRechazarPacienteDuplicadoIgnorandoMayusculas() {
        Paciente existente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
        Paciente duplicado = paciente(" juan ", " perez ", "lopez", "5512345678", "otro@test.com");
        when(pacienteRepository.findAll()).thenReturn(List.of(existente));

        assertThrows(PacienteValidationException.class, () -> service.guardar(duplicado));
        verify(pacienteRepository, never()).save(any());
    }

    @Test
    void debePermitirActualizarElMismoPaciente() {
        Paciente existente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
        when(pacienteRepository.findAll()).thenReturn(List.of(existente));
        when(pacienteRepository.save(existente)).thenReturn(existente);

        assertDoesNotThrow(() -> service.guardar(existente));
        verify(pacienteRepository).save(existente);
    }

    @Test
    void debeEliminarPacienteConYSinCitasYRechazarAusente() {
        Paciente paciente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", null);
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(citaRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.eliminar(7L));
        verify(pacienteRepository).deleteById(7L);

        when(pacienteRepository.findById(8L)).thenReturn(Optional.empty());
        assertThrows(IllegalArgumentException.class, () -> service.eliminar(8L));
    }

    @Test
    void debeRechazarEliminarPacienteConCitas() {
        Paciente paciente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", null);
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        mx.dentalcare.domain.cita.Cita cita = new mx.dentalcare.domain.cita.Cita();
        cita.setPaciente(paciente);
        when(citaRepository.findAll()).thenReturn(List.of(cita));

        assertThrows(IllegalStateException.class, () -> service.eliminar(7L));
        verify(pacienteRepository, never()).deleteById(anyLong());
    }

    @Test
    void debeIgnorarCitasSinPacienteAlEliminar() {
        Paciente paciente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", null);
        mx.dentalcare.domain.cita.Cita cita = new mx.dentalcare.domain.cita.Cita();
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(citaRepository.findAll()).thenReturn(List.of(cita));

        assertDoesNotThrow(() -> service.eliminar(7L));
        verify(pacienteRepository).deleteById(7L);
    }

    @Test
    void debeRechazarEliminarIdNulo() {
        assertThrows(IllegalArgumentException.class, () -> service.eliminar(null));
    }

    private Paciente paciente(String nombre, String paterno, String materno, String telefono, String email) {
        return paciente(null, nombre, paterno, materno, telefono, email);
    }

    private Paciente paciente(Long id, String nombre, String paterno, String materno, String telefono, String email) {
        return new Paciente(id, nombre, paterno, materno, telefono, email);
    }
}
