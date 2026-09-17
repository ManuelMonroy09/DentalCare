package mx.dentalcare;

import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.event.CitaEstadoCambiadoEvent;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class EventTest {

    @Test
    void eventoDebeConservarLaCita() {
        Cita cita = new Cita(new Paciente(1L, "Juan", "Perez", "Lopez", "555", "j@test.com"),
                LocalDateTime.of(2030, 1, 1, 9, 0));
        CitaEstadoCambiadoEvent event = new CitaEstadoCambiadoEvent(cita);
        assertSame(cita, event.getCita());
    }
}
