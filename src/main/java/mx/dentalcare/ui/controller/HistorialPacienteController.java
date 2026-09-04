package mx.dentalcare.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.service.CitaService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class HistorialPacienteController {

    private static final double ANCHO_VENTANA = 980;
    private static final double ALTO_VENTANA = 620;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private Label lblPaciente;
    @FXML private Label lblResumen;
    @FXML private TableView<Cita> historialTable;
    @FXML private TableColumn<Cita, String> fechaColumn;
    @FXML private TableColumn<Cita, String> horarioColumn;
    @FXML private TableColumn<Cita, String> motivoColumn;
    @FXML private TableColumn<Cita, String> tratamientosColumn;
    @FXML private TableColumn<Cita, String> totalColumn;
    @FXML private Button btnDetalle;
    @FXML private Button btnCerrar;

    private final CitaService citaService;
    private final ApplicationContext applicationContext;
    private Paciente paciente;

    public HistorialPacienteController(CitaService citaService, ApplicationContext applicationContext) {
        this.citaService = citaService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        configurarColumnas();
        historialTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, actual) -> btnDetalle.setDisable(actual == null)
        );

        historialTable.setPlaceholder(new Label("Este paciente todavía no tiene consultas atendidas."));
        btnDetalle.setDisable(true);
        btnDetalle.setOnAction(event -> abrirDetalle());
        btnCerrar.setOnAction(event -> cerrarVentana());

        historialTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2 && historialTable.getSelectionModel().getSelectedItem() != null) {
                abrirDetalle();
            }
        });
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
        lblPaciente.setText(obtenerNombrePaciente(paciente));
        cargarHistorial();
        ajustarTamanoVentana();
    }

    private void configurarColumnas() {
        fechaColumn.setCellValueFactory(data -> {
            Cita cita = data.getValue();
            return new SimpleStringProperty(cita != null && cita.getInicio() != null
                    ? cita.getInicio().format(FORMATO_FECHA) : "-");
        });

        horarioColumn.setCellValueFactory(data -> {
            Cita cita = data.getValue();
            return new SimpleStringProperty(cita != null && cita.getInicio() != null && cita.getFin() != null
                    ? cita.getInicio().format(FORMATO_HORA) + " - " + cita.getFin().format(FORMATO_HORA) : "-");
        });

        motivoColumn.setCellValueFactory(data -> {
            Cita cita = data.getValue();
            return new SimpleStringProperty(cita != null && cita.getMotivo() != null && !cita.getMotivo().isBlank()
                    ? cita.getMotivo().trim() : "Sin motivo registrado");
        });

        tratamientosColumn.setCellValueFactory(data ->
                new SimpleStringProperty(obtenerResumenTratamientos(data.getValue())));

        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatearTotal(data.getValue())));

        centrarColumna(fechaColumn);
        centrarColumna(horarioColumn);
        centrarColumna(totalColumn);

        fechaColumn.setPrefWidth(105);
        horarioColumn.setPrefWidth(145);
        motivoColumn.setPrefWidth(270);
        tratamientosColumn.setPrefWidth(260);
        totalColumn.setPrefWidth(100);

        historialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void centrarColumna(TableColumn<?, ?> columna) {
        columna.setStyle("-fx-alignment: CENTER;");
    }

    private void cargarHistorial() {
        if (paciente == null || paciente.getId() == null) {
            historialTable.setItems(FXCollections.observableArrayList());
            lblResumen.setText("Sin información de historial");
            return;
        }

        List<Cita> historial = citaService.obtenerHistorialPorPaciente(paciente.getId());
        historialTable.setItems(FXCollections.observableArrayList(historial));

        int consultas = historial.size();
        BigDecimal total = historial.stream()
                .map(Cita::obtenerTotalTratamientos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblResumen.setText(
                consultas + (consultas == 1 ? " consulta atendida" : " consultas atendidas")
                        + "  ·  Total de tratamientos: $" + total.setScale(2).toPlainString()
        );
    }

    private String obtenerNombrePaciente(Paciente paciente) {
        if (paciente == null) {
            return "Paciente";
        }

        String nombre = paciente.getNombre() != null ? paciente.getNombre().trim() : "";
        String paterno = paciente.getApellidoPaterno() != null ? paciente.getApellidoPaterno().trim() : "";
        String materno = paciente.getApellidoMaterno() != null ? paciente.getApellidoMaterno().trim() : "";
        return (nombre + " " + paterno + " " + materno).trim();
    }

    private String obtenerResumenTratamientos(Cita cita) {
        if (cita == null || cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) {
            return "Sin tratamientos registrados";
        }

        String resumen = cita.getTratamientos().stream()
                .filter(t -> t != null)
                .map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(String::trim)
                .collect(Collectors.joining(", "));

        return resumen.isBlank() ? "Sin tratamientos registrados" : resumen;
    }

    private String formatearTotal(Cita cita) {
        if (cita == null) {
            return "$0.00";
        }
        return "$" + cita.obtenerTotalTratamientos().setScale(2).toPlainString();
    }

    private void abrirDetalle() {
        Cita citaSeleccionada = historialTable.getSelectionModel().getSelectedItem();
        if (citaSeleccionada == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/ui/fxml/DetalleCitaDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);

            Parent root = loader.load();
            DetalleCitaController controller = loader.getController();
            controller.setCita(citaSeleccionada);

            Stage stage = new Stage();
            stage.setTitle("Detalle de cita");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 620, 720));
            stage.setMinWidth(620);
            stage.setMinHeight(720);
            stage.setMaxWidth(620);
            stage.setMaxHeight(720);
            stage.setResizable(false);
            stage.showAndWait();

            cargarHistorial();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void ajustarTamanoVentana() {
        javafx.application.Platform.runLater(() -> {
            if (btnCerrar.getScene() == null || btnCerrar.getScene().getWindow() == null) {
                return;
            }

            Stage stage = (Stage) btnCerrar.getScene().getWindow();
            stage.setMinWidth(ANCHO_VENTANA);
            stage.setMinHeight(ALTO_VENTANA);
            stage.setMaxWidth(ANCHO_VENTANA);
            stage.setMaxHeight(ALTO_VENTANA);
            stage.setWidth(ANCHO_VENTANA);
            stage.setHeight(ALTO_VENTANA);
            stage.setResizable(false);
        });
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }
}
