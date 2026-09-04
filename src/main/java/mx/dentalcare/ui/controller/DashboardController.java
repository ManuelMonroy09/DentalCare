package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.FinanzasService;
import mx.dentalcare.service.PacientesService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class DashboardController {

    @FXML private Label lblTotalPacientes;
    @FXML private Label lblCitasHoy;
    @FXML private Label lblProximaHora;
    @FXML private Label lblProximoPaciente;
    @FXML private Label lblProximoTratamiento;
    @FXML private Label lblProximoEstado;
    @FXML private Label lblIngresosHoy;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblCobrosHoy;

    private final PacientesService pacienteService;
    private final CitaService citaService;
    private final FinanzasService finanzasService;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public DashboardController(PacientesService pacienteService, CitaService citaService,
                               FinanzasService finanzasService) {
        this.pacienteService = pacienteService;
        this.citaService = citaService;
        this.finanzasService = finanzasService;
    }

    @FXML
    public void initialize() {
        cargarResumen();
        cargarProximaCita();
        cargarResumenFinanciero();
    }

    private void cargarResumen() {
        int totalPacientes = pacienteService.obtenerTodos().size();
        long citasHoy = citaService.obtenerTodas().stream()
                .filter(cita -> cita.getInicio() != null)
                .filter(cita -> cita.getInicio().toLocalDate().equals(LocalDate.now()))
                .count();
        lblTotalPacientes.setText(String.valueOf(totalPacientes));
        lblCitasHoy.setText(String.valueOf(citasHoy));
    }

    private void cargarResumenFinanciero() {
        LocalDate hoy = LocalDate.now();
        lblIngresosHoy.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
        lblPorCobrar.setText(moneda(finanzasService.obtenerPorCobrar()));
        lblCobrosHoy.setText(String.valueOf(finanzasService.obtenerCantidadPagos(hoy, hoy)));
    }

    private void cargarProximaCita() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = LocalDate.now();

        Cita proximaCita = citaService.obtenerTodas().stream()
                .filter(cita -> cita.getInicio() != null)
                .filter(cita -> cita.getInicio().toLocalDate().equals(hoy))
                .filter(cita -> cita.getInicio().isAfter(ahora))
                .min(Comparator.comparing(Cita::getInicio))
                .orElse(null);

        if (proximaCita == null) {
            mostrarSinProximaCita();
            return;
        }

        lblProximaHora.setText(proximaCita.getInicio().format(FORMATO_HORA));
        if (proximaCita.getPaciente() != null) {
            lblProximoPaciente.setText(proximaCita.getPaciente().getNombre() + " "
                    + proximaCita.getPaciente().getApellidoPaterno());
        } else {
            lblProximoPaciente.setText("Paciente no disponible");
        }
        lblProximoTratamiento.setText(obtenerResumenTratamientos(proximaCita));
        lblProximoEstado.setText(proximaCita.getEstado() != null ? proximaCita.getEstado().name() : "SIN ESTADO");
    }

    private void mostrarSinProximaCita() {
        lblProximaHora.setText("--:--");
        lblProximoPaciente.setText("No hay próximas citas");
        lblProximoTratamiento.setText("Agenda libre");
        lblProximoEstado.setText("");
    }

    private String obtenerResumenTratamientos(Cita cita) {
        if (cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) return "Sin tratamiento registrado";
        List<String> nombres = cita.getTratamientos().stream()
                .filter(tratamiento -> tratamiento != null)
                .map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .toList();
        if (nombres.isEmpty()) return "Sin tratamiento registrado";
        if (nombres.size() == 1) return nombres.get(0);
        return nombres.get(0) + " +" + (nombres.size() - 1);
    }

    private String moneda(BigDecimal valor) {
        return "$" + (valor == null ? "0.00" : valor.setScale(2).toPlainString());
    }
}
