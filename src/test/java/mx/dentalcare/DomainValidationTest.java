package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
    void citaDebeManejarFechasNulasYDuracionSinInicio() {
        Cita vacia = new Cita();
        assertEquals(0, vacia.getDuracionMinutos());
        assertFalse(vacia.tieneHorarioValido());
        assertThrows(IllegalStateException.class, () -> vacia.establecerDuracion(30));
        assertEquals("", vacia.getNombrePaciente());
        assertDoesNotThrow(() -> vacia.quitarTratamiento(null));
        vacia.setTratamientos(null);
        assertNotNull(vacia.getTratamientos());
        assertEquals(BigDecimal.ZERO, vacia.obtenerTotalTratamientos());
    }

    @Test
    void citaDebeRechazarDuracionInvalida() {
        Cita cita = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0));
        assertThrows(IllegalArgumentException.class, () -> cita.establecerDuracion(0));
        assertThrows(IllegalArgumentException.class, () -> cita.establecerDuracion(-5));
    }

    @Test
    void citaDebeValidarTodosLosCamposObligatorios() {
        Cita cita = new Cita();
        assertThrows(IllegalStateException.class, cita::validar);
        cita.setPaciente(paciente());
        assertThrows(IllegalStateException.class, cita::validar);
        cita.setInicio(LocalDateTime.of(2030, 1, 1, 9, 0));
        assertThrows(IllegalStateException.class, cita::validar);
        cita.setFin(LocalDateTime.of(2030, 1, 1, 9, 30));
        cita.setEstado(null);
        assertThrows(IllegalStateException.class, cita::validar);
        cita.setEstado(EstadoCita.PROGRAMADA);
        assertDoesNotThrow(cita::validar);
        cita.setFin(LocalDateTime.of(2030, 1, 1, 8, 30));
        assertThrows(IllegalStateException.class, cita::validar);
    }

    @Test
    void citaDebeCalcularTotalDeTratamientos() {
        Cita cita = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 9, 0));
        cita.agregarTratamiento(new TratamientoAplicado(1L, "Limpieza", new BigDecimal("500"), 30));
        cita.agregarTratamiento(new TratamientoAplicado(2L, "Revision", new BigDecimal("250"), 20));

        assertEquals(new BigDecimal("750"), cita.obtenerTotalTratamientos());
        assertEquals("Juan Perez Lopez", cita.getNombrePaciente());
        assertThrows(IllegalArgumentException.class, () -> cita.agregarTratamiento(null));
        cita.quitarTratamiento(cita.getTratamientos().get(0));
        assertEquals(1, cita.getTratamientos().size());
    }

    @Test
    void citaDebeNormalizarNombrePacienteYCompararPorId() {
        Paciente incompleto = new Paciente(2L, null, null, "Lopez", null, null);
        Cita cita = new Cita(incompleto, LocalDateTime.of(2030, 1, 1, 9, 0));
        assertEquals("Lopez", cita.getNombrePaciente());

        Cita misma = new Cita(paciente(), LocalDateTime.of(2030, 1, 1, 10, 0));
        cita.setId(5L);
        misma.setId(5L);
        assertTrue(cita.equals(cita));
        assertTrue(cita.equals(misma));
        assertFalse(cita.equals("otro"));
        assertFalse(new Cita().equals(misma));
        assertNotNull(cita.hashCode());
        assertTrue(cita.toString().contains("id=5"));
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
        assertTrue(tratamiento.isActivo());
        tratamiento.desactivar();
        assertFalse(tratamiento.isActivo());
        tratamiento.activar();
        assertTrue(tratamiento.isActivo());
    }

    @Test
    void tratamientoDebeRechazarDatosInvalidosYCubrirIgualdad() {
        Tratamiento sinNombre = new Tratamiento();
        sinNombre.setPrecio(new BigDecimal("100"));
        sinNombre.setDuracionMinutos(30);
        assertThrows(IllegalStateException.class, sinNombre::validar);

        Tratamiento sinPrecio = new Tratamiento();
        sinPrecio.setNombre("Tratamiento");
        sinPrecio.setPrecio(null);
        sinPrecio.setDuracionMinutos(30);
        assertThrows(IllegalStateException.class, sinPrecio::validar);

        Tratamiento precioNegativo = new Tratamiento();
        precioNegativo.setNombre("Tratamiento");
        precioNegativo.setPrecio(new BigDecimal("-1"));
        precioNegativo.setDuracionMinutos(30);
        assertThrows(IllegalStateException.class, precioNegativo::validar);

        Tratamiento duracion = new Tratamiento();
        duracion.setNombre("Tratamiento");
        duracion.setPrecio(BigDecimal.ZERO);
        duracion.setDuracionMinutos(0);
        assertThrows(IllegalStateException.class, duracion::validar);

        Tratamiento a = new Tratamiento();
        Tratamiento b = new Tratamiento();
        assertTrue(a.equals(a));
        assertFalse(a.equals("otro"));
        assertFalse(a.equals(b));
        a.setId(1L); b.setId(1L);
        assertTrue(a.equals(b));
        assertNotNull(a.hashCode());
        assertTrue(a.toString().contains("id=1"));
        a.setNombre(null);
        assertNull(a.getNombre());
    }

    @Test
    void tratamientoAplicadoDebeValidarTodosLosDatos() {
        TratamientoAplicado aplicado = new TratamientoAplicado(1L, "  Consulta   ", new BigDecimal("350"), 30);

        assertEquals("Consulta", aplicado.getNombre());
        assertEquals(new BigDecimal("350"), aplicado.obtenerImporte());
        assertDoesNotThrow(aplicado::validar);
        assertEquals(BigDecimal.ZERO, new TratamientoAplicado().obtenerImporte());
        assertNull(new TratamientoAplicado().getNombre());

        assertThrows(IllegalStateException.class, () -> {
            TratamientoAplicado x = new TratamientoAplicado(); x.setNombre("x"); x.setPrecio(BigDecimal.ONE); x.setDuracionMinutos(1); x.validar();
        });
        assertThrows(IllegalStateException.class, () -> {
            TratamientoAplicado x = new TratamientoAplicado(1L, "", BigDecimal.ONE, 1); x.validar();
        });
        assertThrows(IllegalStateException.class, () -> {
            TratamientoAplicado x = new TratamientoAplicado(1L, "x", null, 1); x.validar();
        });
        assertThrows(IllegalStateException.class, () -> {
            TratamientoAplicado x = new TratamientoAplicado(1L, "x", new BigDecimal("-1"), 1); x.validar();
        });
        assertThrows(IllegalStateException.class, () -> {
            TratamientoAplicado x = new TratamientoAplicado(1L, "x", BigDecimal.ONE, 0); x.validar();
        });

        TratamientoAplicado same = new TratamientoAplicado(1L, "x", BigDecimal.ONE, 1);
        assertTrue(aplicado.equals(aplicado));
        assertFalse(aplicado.equals("otro"));
        assertTrue(aplicado.equals(same));
        assertNotNull(aplicado.hashCode());
        assertTrue(aplicado.toString().contains("tratamientoId=1"));
    }

    @Test
    void cargoDebeValidarDatosObligatorios() {
        Cargo cargo = new Cargo(1L, 2L, LocalDateTime.of(2030, 1, 1, 9, 0), "Consulta", new BigDecimal("500"));
        assertDoesNotThrow(cargo::validar);
        cargo.setImporte(BigDecimal.ZERO);
        assertThrows(IllegalStateException.class, cargo::validar);

        Cargo vacio = new Cargo();
        assertThrows(IllegalStateException.class, vacio::validar);
        vacio.setPacienteId(1L); assertThrows(IllegalStateException.class, vacio::validar);
        vacio.setCitaId(1L); assertThrows(IllegalStateException.class, vacio::validar);
        vacio.setFecha(LocalDateTime.now()); assertThrows(IllegalStateException.class, vacio::validar);
        vacio.setConcepto("Consulta"); vacio.setImporte(BigDecimal.ONE);
        assertDoesNotThrow(vacio::validar);
    }

    @Test
    void pagoDebeDistinguirPagoYAnticipoYCompletarDefaults() {
        Pago pago = new Pago(1L, 2L, LocalDateTime.now(), new BigDecimal("100"), MetodoPago.EFECTIVO, null);
        assertDoesNotThrow(pago::validar);
        assertEquals(mx.dentalcare.domain.financiero.TipoPago.PAGO, pago.getTipo());

        Pago anticipo = Pago.anticipo(1L, 3L, LocalDateTime.now(), new BigDecimal("100"), MetodoPago.EFECTIVO, null);
        assertDoesNotThrow(anticipo::validar);
        assertEquals(mx.dentalcare.domain.financiero.TipoPago.ANTICIPO, anticipo.getTipo());

        Pago defaults = new Pago();
        defaults.setPacienteId(1L); defaults.setCitaId(3L); defaults.setFecha(LocalDateTime.now());
        defaults.setMonto(BigDecimal.ONE); defaults.setMetodoPago(MetodoPago.EFECTIVO);
        defaults.setEstado(null); defaults.setTipo(null);
        defaults.validar();
        assertEquals(mx.dentalcare.domain.financiero.TipoPago.ANTICIPO, defaults.getTipo());
    }

    @Test
    void pagoDebeRechazarTodosLosCamposInvalidos() {
        Pago p = new Pago();
        assertThrows(IllegalStateException.class, p::validar);
        p.setPacienteId(1L); assertThrows(IllegalStateException.class, p::validar);
        p.setCargoId(2L); assertThrows(IllegalStateException.class, p::validar);
        p.setFecha(LocalDateTime.now()); assertThrows(IllegalStateException.class, p::validar);
        p.setMonto(BigDecimal.ZERO); assertThrows(IllegalStateException.class, p::validar);
        p.setMonto(BigDecimal.ONE); assertThrows(IllegalStateException.class, p::validar);
        p.setMetodoPago(MetodoPago.EFECTIVO);
        assertDoesNotThrow(p::validar);
    }

    @Test
    void pagoDebeRechazarMontoNoPositivo() {
        Pago pago = new Pago(1L, 2L, LocalDateTime.now(), BigDecimal.ZERO, MetodoPago.EFECTIVO, null);
        assertThrows(IllegalStateException.class, pago::validar);
        pago.setMonto(new BigDecimal("-1"));
        assertThrows(IllegalStateException.class, pago::validar);
    }

    @Test
    void enumsDeCitaDebenEstarCompletos() {
        assertEquals(5, EstadoCita.values().length);
        assertEquals(EstadoCita.PROGRAMADA, EstadoCita.valueOf("PROGRAMADA"));
    }

    private Paciente paciente() {
        return new Paciente(1L, "Juan", "Perez", "Lopez", "5512345678", "juan@test.com");
    }
}
