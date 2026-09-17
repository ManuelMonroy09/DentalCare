package mx.dentalcare;

import mx.dentalcare.domain.auditoria.AuditEntry;
import mx.dentalcare.domain.configuracion.ConfiguracionConsultorio;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.EstadoPago;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.TipoPago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.paciente.exception.PacienteValidationException;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SimpleDomainObjectsTest {

    @Test
    void configuracionConsultorioDebeExponerValoresInicialesYSetters() {
        ConfiguracionConsultorio c = new ConfiguracionConsultorio();
        assertEquals("DentalCare", c.getNombreConsultorio());
        assertEquals("", c.getNombreOdontologo());
        assertEquals("Gracias por su visita.", c.getPieRecibo());

        c.setNombreConsultorio("Clinica");
        c.setNombreOdontologo("Dra. Ana");
        c.setTelefono("555");
        c.setEmail("a@b.com");
        c.setDireccion("Centro");
        c.setPieRecibo("Gracias");

        assertAll(
                () -> assertEquals("Clinica", c.getNombreConsultorio()),
                () -> assertEquals("Dra. Ana", c.getNombreOdontologo()),
                () -> assertEquals("555", c.getTelefono()),
                () -> assertEquals("a@b.com", c.getEmail()),
                () -> assertEquals("Centro", c.getDireccion()),
                () -> assertEquals("Gracias", c.getPieRecibo())
        );
    }

    @Test
    void pacienteDebeExponerTodosSusCampos() {
        Paciente p = new Paciente(1L, "Juan", "Perez", "Lopez", "555", "j@test.com");
        assertEquals(1L, p.getId());
        assertEquals("Juan", p.getNombre());
        assertEquals("Perez", p.getApellidoPaterno());
        assertEquals("Lopez", p.getApellidoMaterno());
        assertEquals("555", p.getTelefono());
        assertEquals("j@test.com", p.getEmail());

        p.setId(2L); p.setNombre("Ana"); p.setApellidoPaterno("Gomez");
        p.setApellidoMaterno("Diaz"); p.setTelefono("666"); p.setEmail("a@test.com");
        assertAll(
                () -> assertEquals(2L, p.getId()), () -> assertEquals("Ana", p.getNombre()),
                () -> assertEquals("Gomez", p.getApellidoPaterno()), () -> assertEquals("Diaz", p.getApellidoMaterno()),
                () -> assertEquals("666", p.getTelefono()), () -> assertEquals("a@test.com", p.getEmail())
        );
    }

    @Test
    void auditEntryDebeExponerTodosSusCampos() {
        AuditEntry e = new AuditEntry();
        LocalDateTime now = LocalDateTime.now();
        e.setId(1L); e.setFechaHora(now); e.setUsername("juan"); e.setDisplayName("Juan");
        e.setRole("Usuario"); e.setModulo("PACIENTES"); e.setAccion("CREAR"); e.setEntidad("PACIENTE");
        e.setEntidadId(2L); e.setDescripcion("Creado"); e.setValorAnterior("-"); e.setValorNuevo("Paciente #2"); e.setResultado("EXITOSO");

        assertAll(
                () -> assertEquals(1L, e.getId()), () -> assertEquals(now, e.getFechaHora()),
                () -> assertEquals("juan", e.getUsername()), () -> assertEquals("Juan", e.getDisplayName()),
                () -> assertEquals("Usuario", e.getRole()), () -> assertEquals("PACIENTES", e.getModulo()),
                () -> assertEquals("CREAR", e.getAccion()), () -> assertEquals("PACIENTE", e.getEntidad()),
                () -> assertEquals(2L, e.getEntidadId()), () -> assertEquals("Creado", e.getDescripcion()),
                () -> assertEquals("-", e.getValorAnterior()), () -> assertEquals("Paciente #2", e.getValorNuevo()),
                () -> assertEquals("EXITOSO", e.getResultado())
        );
    }

    @Test
    void enumsFinancierosDebenExponerDescripcionYEstadoActivo() {
        assertEquals("Pendiente", EstadoCargo.PENDIENTE.toString());
        assertEquals("Parcial", EstadoCargo.PARCIAL.toString());
        assertEquals("Pagado", EstadoCargo.PAGADO.toString());
        assertEquals("Registrado", EstadoPago.REGISTRADO.toString());
        assertEquals("Cancelado", EstadoPago.CANCELADO.toString());
        assertTrue(EstadoPago.REGISTRADO.estaActivo());
        assertFalse(EstadoPago.CANCELADO.estaActivo());
        assertEquals("Efectivo", MetodoPago.EFECTIVO.toString());
        assertEquals("Tarjeta", MetodoPago.TARJETA.toString());
        assertEquals("Transferencia", MetodoPago.TRANSFERENCIA.toString());
        assertEquals("Otro", MetodoPago.OTRO.toString());
        assertEquals("Anticipo", TipoPago.ANTICIPO.getDescripcion());
        assertEquals("Pago", TipoPago.PAGO.getDescripcion());
    }

    @Test
    void excepcionDeValidacionDebeConservarElMensaje() {
        PacienteValidationException ex = new PacienteValidationException("dato invalido");
        assertEquals("dato invalido", ex.getMessage());
    }
}
