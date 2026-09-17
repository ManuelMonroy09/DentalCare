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
    void debeRechazarNombreInvalido() {
        Paciente paciente = paciente("J1", "Perez", "Lopez", "5512345678", null);
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente));
    }

    @Test
    void debeRechazarTelefonoQueNoSeaDeDiezDigitos() {
        Paciente paciente = paciente("Juan", "Perez", "Lopez", "551234", null);
        assertThrows(PacienteValidationException.class, () -> service.guardar(paciente));
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
        existente.setNombre("Juan Carlos");
        when(pacienteRepository.findAll()).thenReturn(List.of(existente));
        when(pacienteRepository.save(existente)).thenReturn(existente);

        assertDoesNotThrow(() -> service.guardar(existente));
        verify(pacienteRepository).save(existente);
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
    void debeEliminarPacienteSinCitas() {
        Paciente paciente = paciente(7L, "Juan", "Perez", "Lopez", "5512345678", null);
        when(pacienteRepository.findById(7L)).thenReturn(Optional.of(paciente));
        when(citaRepository.findAll()).thenReturn(List.of());

        assertDoesNotThrow(() -> service.eliminar(7L));
        verify(pacienteRepository).deleteById(7L);
        verify(auditService).registrar(eq("PACIENTES"), eq("ELIMINAR"), eq("PACIENTE"), eq(7L),
                anyString(), eq("Paciente #7"), isNull(), eq("EXITOSO"));
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
