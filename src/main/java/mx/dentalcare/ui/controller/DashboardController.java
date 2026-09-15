package mx.dentalcare.ui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.util.Duration;
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
    @FXML private Label lblResumenCitas;
    @FXML private Label lblProximaHora;
    @FXML private Label lblProximoPaciente;
    @FXML private Label lblProximoTratamiento;
    @FXML private Label lblProximoEstado;
    @FXML private Label lblIngresosHoy;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblCobrosHoy;
    @FXML private Label lblFecha;
    @FXML private Label lblReloj;

    private final PacientesService pacienteService;
    private final CitaService citaService;
    private final FinanzasService finanzasService;
    private Timeline relojTimeline;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final DateTimeFormatter FORMATO_FECHA =
            DateTimeFormatter.ofPattern("EEEE, dd 'de' MMMM 'de' yyyy", java.util.Locale.forLanguageTag("es-MX"));

    public DashboardController(PacientesService pacienteService, CitaService citaService,
                               FinanzasService finanzasService) {
        this.pacienteService = pacienteService;
        this.citaService = citaService;
        this.finanzasService = finanzasService;
    }

    @FXML
    public void initialize() {
        inicializarValoresSeguros();
        cargarResumenSeguro();
        cargarProximaCitaSegura();
        cargarResumenFinancieroSeguro();
        iniciarReloj();
    }

    private void inicializarValoresSeguros() {
        lblTotalPacientes.setText("0");
        lblCitasHoy.setText("0");
        lblResumenCitas.setText("0");
        lblIngresosHoy.setText("$0.00");
        lblPorCobrar.setText("$0.00");
        lblCobrosHoy.setText("0");
        mostrarSinProximaCita();
    }

    private void cargarResumenSeguro() {
        try {
            int totalPacientes = pacienteService.obtenerTodos().size();
            long citasHoy = contarCitasHoy();
            lblTotalPacientes.setText(String.valueOf(totalPacientes));
            lblCitasHoy.setText(String.valueOf(citasHoy));
            lblResumenCitas.setText(String.valueOf(citasHoy));
        } catch (Exception e) {
            // El dashboard no debe impedir la apertura de DentalCare si falla un dato del resumen.
        }
    }

    private void cargarResumenFinancieroSeguro() {
        try {
            LocalDate hoy = LocalDate.now();
            lblIngresosHoy.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
            lblPorCobrar.setText(moneda(finanzasService.obtenerPorCobrar()));
            lblCobrosHoy.setText(String.valueOf(finanzasService.obtenerCantidadPagos(hoy, hoy)));
        } catch (Exception e) {
            // Se mantienen los valores seguros iniciales si Finanzas no está disponible.
        }
    }

    private void cargarProximaCitaSegura() {
        try {
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
            lblProximoEstado.setText(proximaCita.getEstado() != null
                    ? proximaCita.getEstado().name() : "SIN ESTADO");
        } catch (Exception e) {
            mostrarSinProximaCita();
        }
    }

    private long contarCitasHoy() {
        return citaService.obtenerTodas().stream()
                .filter(cita -> cita.getInicio() != null)
                .filter(cita -> cita.getInicio().toLocalDate().equals(LocalDate.now()))
                .count();
    }

    private void iniciarReloj() {
        try {
            actualizarReloj();
            relojTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> actualizarReloj()));
            relojTimeline.setCycleCount(Timeline.INDEFINITE);
            relojTimeline.play();
        } catch (Exception e) {
            // El reloj es accesorio y no debe bloquear el dashboard.
        }
    }

    private void actualizarReloj() {
        LocalDateTime ahora = LocalDateTime.now();
        lblReloj.setText(ahora.format(FORMATO_HORA));
        lblFecha.setText(ahora.format(FORMATO_FECHA));
    }

    private void mostrarSinProximaCita() {
        lblProximaHora.setText("--:--");
        lblProximoPaciente.setText("No hay próximas citas");
        lblProximoTratamiento.setText("Agenda libre");
        lblProximoEstado.setText("");
    }

    private String obtenerResumenTratamientos(Cita cita) {
        if (cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) {
            return "Sin tratamiento registrado";
        }
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
