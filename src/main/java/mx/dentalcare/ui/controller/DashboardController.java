package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.PacientesService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class DashboardController {

    @FXML
    private Label lblTotalPacientes;

    @FXML
    private Label lblCitasHoy;

    @FXML
    private Label lblProximaHora;

    @FXML
    private Label lblProximoPaciente;

    @FXML
    private Label lblProximoMotivo;

    @FXML
    private Label lblProximoEstado;

    @FXML
    private VBox contenedorCitasHoy;

    private final PacientesService pacienteService;
    private final CitaService citaService;

    private static final DateTimeFormatter FORMATO_HORA =
            DateTimeFormatter.ofPattern("HH:mm");

    public DashboardController(PacientesService pacienteService, CitaService citaService) {
        this.pacienteService = pacienteService;
        this.citaService = citaService;
    }

    @FXML
    public void initialize() {
        cargarResumen();
        cargarProximaCita();
        cargarCitasHoy();
    }

    private void cargarResumen() {
        int totalPacientes = pacienteService.obtenerTodos().size();
        long citasHoy = citaService.obtenerTodas().stream().filter(cita -> cita.getInicio() != null).filter(cita -> cita.getInicio().toLocalDate().equals(LocalDate.now())).count();
        lblTotalPacientes.setText(String.valueOf(totalPacientes));
        lblCitasHoy.setText(String.valueOf(citasHoy));
    }

    private void cargarProximaCita() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = LocalDate.now();
        Cita proximaCita = citaService.obtenerTodas().stream().filter(cita -> cita.getInicio() != null).filter(cita -> cita.getInicio().toLocalDate().equals(hoy)).filter(cita -> cita.getInicio().isAfter(ahora)).min(Comparator.comparing(Cita::getInicio)).orElse(null);
        if (proximaCita == null) {
            mostrarSinProximaCita();
            return;
        }
        lblProximaHora.setText(proximaCita.getInicio().format(FORMATO_HORA));
        if (proximaCita.getPaciente() != null) {
            String nombre = proximaCita.getPaciente().getNombre() + " " + proximaCita.getPaciente().getApellidoPaterno();
            lblProximoPaciente.setText(nombre);
        } else {
            lblProximoPaciente.setText("Paciente no disponible");
        }
        lblProximoMotivo.setText(proximaCita.getMotivo() != null ? proximaCita.getMotivo() : "Sin motivo");
        lblProximoEstado.setText(proximaCita.getEstado() != null ? proximaCita.getEstado().name() : "SIN ESTADO");
    }

    private void mostrarSinProximaCita() {
        lblProximaHora.setText("--:--");
        lblProximoPaciente.setText("No hay próximas citas");
        lblProximoMotivo.setText("Agenda libre");
        lblProximoEstado.setText("");
    }

    private void cargarCitasHoy() {
        contenedorCitasHoy.getChildren().clear();
        LocalDate hoy = LocalDate.now();
        List<Cita> citasHoy = citaService.obtenerTodas().stream().filter(cita -> cita.getInicio() != null).filter(cita -> cita.getInicio().toLocalDate().equals(hoy)).sorted(Comparator.comparing(Cita::getInicio)).toList();
        if (citasHoy.isEmpty()) {
            Label mensaje = new Label("No hay citas programadas para hoy.");
            mensaje.getStyleClass().add("page-subtitle");
            contenedorCitasHoy.getChildren().add(mensaje);
            return;
        }
        for (Cita cita : citasHoy) {
            contenedorCitasHoy.getChildren().add(crearFilaCita(cita));
        }
    }

    private HBox crearFilaCita(Cita cita) {
        HBox fila = new HBox(15);
        fila.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        fila.getStyleClass().add("dashboard-appointment");
        Label hora = new Label(cita.getInicio().format(FORMATO_HORA));
        hora.getStyleClass().add("dashboard-appointment-time");
        VBox informacion = new VBox(3);
        String nombrePaciente;
        if (cita.getPaciente() != null) {
            nombrePaciente = cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidoPaterno();
        } else {
            nombrePaciente = "Paciente no disponible";
        }
        Label paciente = new Label(nombrePaciente);
        paciente.getStyleClass().add("dashboard-appointment-patient");
        Label motivo = new Label(cita.getMotivo() != null ? cita.getMotivo() : "Sin motivo");
        motivo.getStyleClass().add("dashboard-appointment-reason");
        informacion.getChildren().addAll(paciente, motivo);
        HBox.setHgrow(informacion, Priority.ALWAYS);
        Label estado = new Label(cita.getEstado() != null ? cita.getEstado().name() : "SIN ESTADO");
        estado.getStyleClass().add("dashboard-appointment-status");
        aplicarEstiloEstado(estado, cita);
        fila.getChildren().addAll(hora, informacion, estado);
        return fila;
    }

    private void aplicarEstiloEstado(Label estado, Cita cita) {
        if (cita.getEstado() == null) {
            estado.getStyleClass().add("status-default");
            return;
        }
        switch (cita.getEstado()) {
            case PROGRAMADA ->
                    estado.getStyleClass().add("status-programada");
            case CONFIRMADA ->
                    estado.getStyleClass().add("status-confirmada");
            case ATENDIDA ->
                    estado.getStyleClass().add("status-atendida");
            case CANCELADA ->
                    estado.getStyleClass().add("status-cancelada");
            case NO_ASISTIO ->
                    estado.getStyleClass().add("status-no-asistio");
            default ->
                    estado.getStyleClass().add("status-default");
        }
    }
}