package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class DomainValidationTest {

    @Test
    void citaDebeCalcularDuracionYValidarHorario() {
        Paciente paciente = paciente();
        LocalDateTime inicio = LocalDateTime.of(2030, 1, 1, 9, 0);
        Cita cita = new Cita(paciente, inicio);

        assertEquals(60, cita.getDuracionMinutos());
        assertTrue(cita.tieneHorarioValido());
        assertDoesNotThrow(cita::validar);

        cita.establecerDuracion(90);
        assertEquals(90, cita.getDuracionMinutos());
    }

    @Test
    void citaDebeRechazarDuracionInvalida() {
        Cita cita = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0));
        assertThrows(IllegalArgumentException.class, () -> cita.establecerDuracion(0));
        assertThrows(IllegalArgumentException.class, () -> cita.establecerDuracion(-5));
    }

    @Test
    void citaDebeCalcularTotalDeTratamientos() {
        Cita cita = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0));
        cita.agregarTratamiento(new TratamientoAplicado(1L, "Limpieza", new BigDecimal("500"), 30));
        cita.agregarTratamiento(new TratamientoAplicado(2L, "Revision", new BigDecimal("250"), 20));

        assertEquals(new BigDecimal("750"), cita.obtenerTotalTratamientos());
        assertEquals("Juan Perez Lopez", cita.getNombrePaciente());
    }

    @Test
    void tratamientoDebeNormalizarTextoYValidarDatos() {
        Tratamiento tratamiento = new Tratamiento();
        tratamiento.setNombre("  Limpieza    dental  ");
        tratamiento.setDescripcion("  Limpieza   general  ");
        tratamiento.setPrecio(new BigDecimal("500"));
        tratamiento.setDuracionMinutos(30);

        assertEquals("Limpieza dental", tratamiento.getNombre());
        assertEquals("Limpieza general", tratamiento.getDescripcion());
        assertDoesNotThrow(tratamiento::validar);
    }

    @Test
    void tratamientoDebeRechazarDatosInvalidos() {
        Tratamiento sinNombre = new Tratamiento();
        sinNombre.setPrecio(new BigDecimal("100"));
        sinNombre.setDuracionMinutos(30);
        assertThrows(IllegalStateException.class, sinNombre::validar);

        Tratamiento precioNegativo = new Tratamiento();
        precioNegativo.setNombre("Tratamiento");
        precioNegativo.setPrecio(new BigDecimal("-1"));
        precioNegativo.setDuracionMinutos(30);
        assertThrows(IllegalStateException.class, precioNegativo::validar);
    }

    @Test
    void tratamientoAplicadoDebeValidarYCalcularImporte() {
        TratamientoAplicado aplicado = new TratamientoAplicado(1L, "  Consulta   ", new BigDecimal("350"), 30);

        assertEquals("Consulta", aplicado.getNombre());
        assertEquals(new BigDecimal("350"), aplicado.obtenerImporte());
        assertDoesNotThrow(aplicado::validar);
    }

    @Test
    void cargoDebeValidarDatosObligatorios() {
        Cargo cargo = new Cargo(1L, 2L, LocalDateTime.of(2030, 1, 1, 9, 0), "Consulta", new BigDecimal("500"));
        assertDoesNotThrow(cargo::validar);

        cargo.setImporte(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, cargo::validar);
    }

    @Test
    void pagoDebeDistinguirPagoYAnticipo() {
        Pago pago = new Pago(1L, 2L, LocalDateTime.now(), new BigDecimal("100"), mx.dentalcare.domain.financiero.MetodoPago.EFECTIVO, null);
        assertDoesNotThrow(pago::validar);
        assertEquals(mx.dentalcare.domain.financiero.TipoPago.PAGO, pago.getTipo());

        Pago anticipo = Pago.anticipo(1L, 3L, LocalDateTime.now(), new BigDecimal("100"), mx.dentalcare.domain.financiero.MetodoPago.EFECTIVO, null);
        assertDoesNotThrow(anticipo::validar);
        assertEquals(mx.dentalcare.domain.financiero.TipoPago.ANTICIPO, anticipo.getTipo());
    }

    @Test
    void pagoDebeRechazarMontoNoPositivo() {
        Pago pago = new Pago(1L, 2L, LocalDateTime.now(), BigDecimal.ZERO, mx.dentalcare.domain.financiero.MetodoPago.EFECTIVO, null);
        assertThrows(IllegalStateException.class, pago::validar);
    }

    private Paciente paciente() {
        return new Paciente(1L, "Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
    }
}
