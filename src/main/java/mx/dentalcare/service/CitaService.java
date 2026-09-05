package mx.dentalcare.service;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.event.CitaEstadoCambiadoEvent;
import mx.dentalcare.repository.CargoRepository;
import mx.dentalcare.repository.CitaRepository;
import mx.dentalcare.repository.PagoRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CitaService {

    private static final long DURACION_POR_DEFECTO_MINUTOS = 60;
    private final CitaRepository citaRepository;
    private final TratamientoService tratamientoService;
    private final CargoRepository cargoRepository;
    private final PagoRepository pagoRepository;
    private final ApplicationEventPublisher eventPublisher;

    public CitaService(CitaRepository citaRepository, TratamientoService tratamientoService,
                       CargoRepository cargoRepository, PagoRepository pagoRepository,
                       ApplicationEventPublisher eventPublisher) {
        this.citaRepository = citaRepository;
        this.tratamientoService = tratamientoService;
        this.cargoRepository = cargoRepository;
        this.pagoRepository = pagoRepository;
        this.eventPublisher = eventPublisher;
    }

    public List<Cita> obtenerTodas() {
        return citaRepository.findAll().stream()
                .sorted(Comparator.comparing(Cita::getInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .collect(Collectors.toList());
    }

    public Optional<Cita> obtenerPorId(Long id) {
        if (id == null) return Optional.empty();
        return citaRepository.findById(id);
    }

    public Cita crear(Paciente paciente, LocalDateTime inicio) { return crear(paciente, inicio, DURACION_POR_DEFECTO_MINUTOS); }

    public Cita crear(Paciente paciente, LocalDateTime inicio, long duracionMinutos) {
        return crear(paciente, inicio, duracionMinutos, null, null);
    }

    public Cita crear(Paciente paciente, LocalDateTime inicio, long duracionMinutos, String motivo, String notas) {
        validarPaciente(paciente);
        validarInicio(inicio);
        validarDuracion(duracionMinutos);
        LocalDateTime fin = inicio.plusMinutes(duracionMinutos);
        Cita cita = new Cita(paciente, inicio, fin);
        cita.setEstado(EstadoCita.PROGRAMADA);
        if (motivo != null && !motivo.isBlank()) cita.setMotivo(motivo.trim());
        if (notas != null && !notas.isBlank()) cita.setNotas(notas.trim());
        validarSolapamiento(cita);
        return cita;
    }

    public Cita guardar(Cita cita) {
        validarCita(cita);
        validarSolapamiento(cita);
        return citaRepository.save(cita);
    }

    public Cita guardarConTratamientos(Cita cita, List<Long> tratamientoIds) {
        validarCita(cita);
        validarTratamientosModificables(cita.getId());
        sincronizarTratamientos(cita, tratamientoIds);
        validarSolapamiento(cita);
        return citaRepository.save(cita);
    }

    private void sincronizarTratamientos(Cita cita, List<Long> tratamientoIds) {
        if (cita.getTratamientos() == null) cita.setTratamientos(new ArrayList<>());
        Set<Long> seleccionados = tratamientoIds == null ? new HashSet<>() : new HashSet<>(tratamientoIds);
        cita.getTratamientos().removeIf(tratamientoAplicado ->
                tratamientoAplicado == null || tratamientoAplicado.getTratamientoId() == null
                        || !seleccionados.contains(tratamientoAplicado.getTratamientoId()));
        Set<Long> existentes = new HashSet<>();
        for (TratamientoAplicado tratamientoAplicado : cita.getTratamientos()) {
            if (tratamientoAplicado != null && tratamientoAplicado.getTratamientoId() != null) existentes.add(tratamientoAplicado.getTratamientoId());
        }
        for (Long tratamientoId : seleccionados) {
            if (tratamientoId == null || existentes.contains(tratamientoId)) continue;
            Tratamiento tratamiento = tratamientoService.obtenerPorId(tratamientoId);
            if (!tratamiento.isActivo()) throw new IllegalStateException("No se puede aplicar un tratamiento inactivo.");
            cita.agregarTratamiento(new TratamientoAplicado(tratamiento.getId(), tratamiento.getNombre(), tratamiento.getPrecio(), tratamiento.getDuracionMinutos()));
            existentes.add(tratamientoId);
        }
    }

    public Cita cambiarInicio(Long id, LocalDateTime nuevoInicio) {
        Cita cita = obtenerExistente(id);
        validarInicio(nuevoInicio);
        long duracion = cita.getDuracionMinutos();
        if (duracion <= 0) duracion = DURACION_POR_DEFECTO_MINUTOS;
        cita.setInicio(nuevoInicio);
        cita.setFin(nuevoInicio.plusMinutes(duracion));
        return guardar(cita);
    }

    public Cita cambiarDuracion(Long id, long duracionMinutos) {
        Cita cita = obtenerExistente(id);
        validarDuracion(duracionMinutos);
        cita.establecerDuracion(duracionMinutos);
        return guardar(cita);
    }

    public Cita agregarTratamiento(Long citaId, Long tratamientoId) {
        Cita cita = obtenerExistente(citaId);
        validarTratamientosModificables(citaId);
        if (tratamientoId == null) throw new IllegalArgumentException("El identificador del tratamiento es obligatorio.");
        Tratamiento tratamiento = tratamientoService.obtenerPorId(tratamientoId);
        if (!tratamiento.isActivo()) throw new IllegalStateException("No se puede aplicar un tratamiento inactivo.");
        cita.agregarTratamiento(new TratamientoAplicado(tratamiento.getId(), tratamiento.getNombre(), tratamiento.getPrecio(), tratamiento.getDuracionMinutos()));
        return citaRepository.save(cita);
    }

    public Cita quitarTratamiento(Long citaId, Long tratamientoId) {
        Cita cita = obtenerExistente(citaId);
        validarTratamientosModificables(citaId);
        if (tratamientoId == null) throw new IllegalArgumentException("El identificador del tratamiento es obligatorio.");
        if (cita.getTratamientos() == null) return cita;
        cita.getTratamientos().removeIf(tratamiento -> tratamiento != null && tratamientoId.equals(tratamiento.getTratamientoId()));
        return citaRepository.save(cita);
    }

    public List<TratamientoAplicado> obtenerTratamientos(Long citaId) {
        Cita cita = obtenerExistente(citaId);
        if (cita.getTratamientos() == null) return List.of();
        return List.copyOf(cita.getTratamientos());
    }

    public List<Cita> obtenerPorFecha(LocalDate fecha) {
        if (fecha == null) throw new IllegalArgumentException("La fecha no puede ser nula.");
        return obtenerPorRango(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
    }

    public List<Cita> obtenerPorRango(LocalDateTime inicio, LocalDateTime fin) {
        validarRango(inicio, fin);
        return citaRepository.findAll().stream().filter(cita -> intersecta(cita.getInicio(), cita.getFin(), inicio, fin))
                .sorted(Comparator.comparing(Cita::getInicio, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
    }

    public List<Cita> obtenerPorPaciente(Long pacienteId) {
        if (pacienteId == null) throw new IllegalArgumentException("El identificador del paciente no puede ser nulo.");
        return citaRepository.findAll().stream().filter(cita -> cita.getPaciente() != null && cita.getPaciente().getId() != null && cita.getPaciente().getId().equals(pacienteId))
                .sorted(Comparator.comparing(Cita::getInicio, Comparator.nullsLast(Comparator.naturalOrder()))).collect(Collectors.toList());
    }

    public List<Cita> obtenerHistorial() {
        return obtenerTodas().stream().filter(cita -> cita.getEstado() == EstadoCita.ATENDIDA)
                .sorted(Comparator.comparing(Cita::getInicio, Comparator.nullsLast(Comparator.reverseOrder()))).collect(Collectors.toList());
    }

    public List<Cita> obtenerHistorialPorPaciente(Long pacienteId) {
        return obtenerHistorial().stream().filter(cita -> cita.getPaciente() != null && cita.getPaciente().getId() != null && cita.getPaciente().getId().equals(pacienteId)).collect(Collectors.toList());
    }

    public Cita cancelar(Long id) { Cita cita = obtenerExistente(id); cita.setEstado(EstadoCita.CANCELADA); return citaRepository.save(cita); }

    public Cita confirmar(Long id) {
        Cita cita = obtenerExistente(id);
        cita.setEstado(EstadoCita.CONFIRMADA);
        Cita guardada = citaRepository.save(cita);
        publicarCambioEstado(guardada);
        return guardada;
    }

    public Cita marcarAtendida(Long id) {
        Cita cita = obtenerExistente(id);
        cita.setEstado(EstadoCita.ATENDIDA);
        Cita guardada = citaRepository.save(cita);
        publicarCambioEstado(guardada);
        return guardada;
    }

    public Cita marcarNoAsistio(Long id) { Cita cita = obtenerExistente(id); cita.setEstado(EstadoCita.NO_ASISTIO); return citaRepository.save(cita); }

    private void publicarCambioEstado(Cita cita) {
        if (cita != null && (cita.getEstado() == EstadoCita.CONFIRMADA || cita.getEstado() == EstadoCita.ATENDIDA)) eventPublisher.publishEvent(new CitaEstadoCambiadoEvent(cita));
    }

    public void eliminar(Long id) {
        if (id == null) throw new IllegalArgumentException("El identificador de la cita no puede ser nulo.");
        Cita cita = obtenerExistente(id);

        if (cargoRepository.findByCitaId(id).isPresent()) {
            mostrarAvisoEliminacion("No se puede eliminar esta cita porque ya tiene un cargo financiero asociado.\n\nPuedes cancelarla para conservar su historial.");
            return;
        }

        boolean tieneMovimientos = pagoRepository.findAll().stream().anyMatch(p -> id.equals(p.getCitaId()));
        if (tieneMovimientos) {
            mostrarAvisoEliminacion("No se puede eliminar esta cita porque tiene movimientos financieros.\n\nCancélala para conservar el historial del anticipo.");
            return;
        }

        citaRepository.deleteById(cita.getId());
    }

    private void mostrarAvisoEliminacion(String mensaje) {
        Runnable mostrar = () -> {
            Alert alerta = new Alert(Alert.AlertType.WARNING);
            alerta.setTitle("DentalCare | Eliminación");
            alerta.setHeaderText("La cita no puede eliminarse");
            alerta.setContentText(mensaje);
            alerta.showAndWait();
        };

        if (Platform.isFxApplicationThread()) {
            mostrar.run();
        } else {
            Platform.runLater(mostrar);
        }
    }

    private void validarTratamientosModificables(Long citaId) {
        if (citaId != null && cargoRepository.findByCitaId(citaId).isPresent()) {
            throw new IllegalStateException("Los tratamientos de una cita con cargo financiero ya generado no pueden modificarse, porque el cargo conserva el importe histórico.");
        }
    }

    private void validarCita(Cita cita) {
        if (cita == null) throw new IllegalArgumentException("La cita no puede ser nula.");
        validarPaciente(cita.getPaciente());
        validarInicio(cita.getInicio());
        if (cita.getFin() == null) throw new IllegalArgumentException("La cita debe tener una fecha y hora de finalización.");
        if (!cita.getFin().isAfter(cita.getInicio())) throw new IllegalArgumentException("La fecha y hora de finalización debe ser posterior al inicio.");
        if (cita.getEstado() == null) cita.setEstado(EstadoCita.PROGRAMADA);
        if (cita.getTratamientos() != null) cita.getTratamientos().forEach(TratamientoAplicado::validar);
    }

    private void validarPaciente(Paciente paciente) {
        if (paciente == null) throw new IllegalArgumentException("El paciente es obligatorio para crear una cita.");
        if (paciente.getId() == null) throw new IllegalArgumentException("El paciente debe estar guardado antes de crear una cita.");
    }

    private void validarInicio(LocalDateTime inicio) { if (inicio == null) throw new IllegalArgumentException("La fecha y hora de inicio son obligatorias."); }
    private void validarDuracion(long duracionMinutos) { if (duracionMinutos <= 0) throw new IllegalArgumentException("La duración de la cita debe ser mayor a 0 minutos."); }

    private void validarRango(LocalDateTime inicio, LocalDateTime fin) {
        if (inicio == null || fin == null) throw new IllegalArgumentException("El inicio y fin del rango son obligatorios.");
        if (!fin.isAfter(inicio)) throw new IllegalArgumentException("El fin del rango debe ser posterior al inicio.");
    }

    private boolean intersecta(LocalDateTime inicioA, LocalDateTime finA, LocalDateTime inicioB, LocalDateTime finB) {
        if (inicioA == null || finA == null || inicioB == null || finB == null) return false;
        return inicioA.isBefore(finB) && finA.isAfter(inicioB);
    }

    private void validarSolapamiento(Cita cita) {
        if (cita == null || cita.getInicio() == null || cita.getFin() == null) return;
        boolean existeConflicto = citaRepository.findAll().stream().filter(existente -> {
            if (existente.getInicio() == null || existente.getFin() == null) return false;
            if (existente.getEstado() == EstadoCita.CANCELADA) return false;
            if (cita.getId() != null && existente.getId() != null && existente.getId().equals(cita.getId())) return false;
            return true;
        }).anyMatch(existente -> intersecta(cita.getInicio(), cita.getFin(), existente.getInicio(), existente.getFin()));
        if (existeConflicto) throw new IllegalStateException("El horario seleccionado entra en conflicto con otra cita.");
    }

    private Cita obtenerExistente(Long id) {
        if (id == null) throw new IllegalArgumentException("El identificador de la cita no puede ser nulo.");
        return citaRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No existe una cita con el identificador: " + id));
    }
}
