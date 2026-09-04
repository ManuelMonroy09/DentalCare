package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.repository.CitaRepository;
import mx.dentalcare.service.CitaService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CitaServiceTest {

    @Autowired
    private CitaService citaService;

    @Autowired
    private CitaRepository citaRepository;

    private Paciente paciente;

    @BeforeEach
    void prepararPaciente() {

        /*
         * Paciente ficticio.
         *
         * No se guarda en pacientes.dat.
         * Únicamente necesitamos un Paciente con ID para
         * satisfacer las reglas de CitaService.
         */
        paciente = new Paciente();

        paciente.setId(999998L);
        paciente.setNombre("Paciente");
        paciente.setApellidoPaterno("Prueba");
        paciente.setApellidoMaterno("Servicio");
        paciente.setTelefono("5555555556");
        paciente.setEmail("servicio@dentalcare.test");

        /*
         * Limpiamos las citas creadas por pruebas anteriores
         * utilizando únicamente las citas de este paciente.
         */
        citaRepository.findAll()
                .stream()
                .filter(cita ->
                        cita.getPaciente() != null
                                && cita.getPaciente().getId() != null
                                && cita.getPaciente().getId().equals(paciente.getId())
                )
                .forEach(cita ->
                        citaRepository.deleteById(cita.getId())
                );
    }

    @Test
    void debeCrearCitaConDuracionDe60MinutosPorDefecto() {

        LocalDateTime inicio = LocalDateTime.of(
                2030,
                2,
                10,
                9,
                0
        );

        Cita cita = citaService.crear(
                paciente,
                inicio
        );

        assertNotNull(cita);
        assertNotNull(cita.getId());

        assertEquals(
                inicio,
                cita.getInicio()
        );

        assertEquals(
                inicio.plusMinutes(60),
                cita.getFin()
        );

        assertEquals(
                60,
                cita.getDuracionMinutos()
        );

        assertEquals(
                EstadoCita.PROGRAMADA,
                cita.getEstado()
        );

        citaRepository.deleteById(cita.getId());
    }

    @Test
    void debePermitirDuracionPersonalizada() {

        LocalDateTime inicio = LocalDateTime.of(
                2030,
                2,
                11,
                10,
                0
        );

        Cita cita = citaService.crear(
                paciente,
                inicio,
                90
        );

        assertNotNull(cita);

        assertEquals(
                inicio,
                cita.getInicio()
        );

        assertEquals(
                inicio.plusMinutes(90),
                cita.getFin()
        );

        assertEquals(
                90,
                cita.getDuracionMinutos()
        );

        citaRepository.deleteById(cita.getId());
    }

    @Test
    void debeRechazarCitaQueSeSolapaConOtra() {

        LocalDateTime inicio = LocalDateTime.of(
                2030,
                2,
                12,
                9,
                0
        );

        Cita primera = citaService.crear(
                paciente,
                inicio
        );

        assertThrows(
                IllegalStateException.class,
                () -> citaService.crear(
                        paciente,
                        inicio.plusMinutes(30)
                )
        );

        citaRepository.deleteById(primera.getId());
    }

    @Test
    void debePermitirCitaQueComienzaCuandoTerminaLaAnterior() {

        LocalDateTime inicio = LocalDateTime.of(
                2030,
                2,
                13,
                9,
                0
        );

        Cita primera = citaService.crear(
                paciente,
                inicio
        );

        Cita segunda = citaService.crear(
                paciente,
                inicio.plusMinutes(60)
        );

        assertNotNull(primera);
        assertNotNull(segunda);

        assertEquals(
                primera.getFin(),
                segunda.getInicio()
        );

        citaRepository.deleteById(primera.getId());
        citaRepository.deleteById(segunda.getId());
    }

    @Test
    void citaCanceladaNoDebeBloquearHorario() {

        LocalDateTime inicio = LocalDateTime.of(
                2030,
                2,
                14,
                11,
                0
        );

        Cita primera = citaService.crear(
                paciente,
                inicio
        );

        citaService.cancelar(
                primera.getId()
        );

        Cita segunda = citaService.crear(
                paciente,
                inicio.plusMinutes(30)
        );

        assertNotNull(segunda);

        assertEquals(
                EstadoCita.CANCELADA,
                citaService.obtenerPorId(primera.getId())
                        .orElseThrow()
                        .getEstado()
        );

        citaRepository.deleteById(primera.getId());
        citaRepository.deleteById(segunda.getId());
    }
}