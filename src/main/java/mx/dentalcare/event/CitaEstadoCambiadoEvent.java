package mx.dentalcare.event;

import mx.dentalcare.domain.cita.Cita;

/**
 * Evento publicado cuando una cita cambia a un estado relevante para otros módulos.
 */
public class CitaEstadoCambiadoEvent {

    private final Cita cita;

    public CitaEstadoCambiadoEvent(Cita cita) {
        this.cita = cita;
    }

    public Cita getCita() {
        return cita;
    }
}
