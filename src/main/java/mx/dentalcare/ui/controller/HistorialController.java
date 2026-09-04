package mx.dentalcare.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.service.CitaService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class HistorialController {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    @FXML private TextField buscarField;
    @FXML private ComboBox<String> pacienteCombo;
    @FXML private ComboBox<String> tratamientoCombo;
    @FXML private DatePicker fechaDesdePicker;
    @FXML private DatePicker fechaHastaPicker;
    @FXML private Button limpiarFiltrosButton;
    @FXML private Button btnDetalle;
    @FXML private TableView<Cita> historialTable;
    @FXML private TableColumn<Cita, String> fechaColumn;
    @FXML private TableColumn<Cita, String> pacienteColumn;
    @FXML private TableColumn<Cita, String> horarioColumn;
    @FXML private TableColumn<Cita, String> motivoColumn;
    @FXML private TableColumn<Cita, String> tratamientosColumn;
    @FXML private TableColumn<Cita, String> totalColumn;
    @FXML private Label resumenLabel;
    @FXML private Label vacioLabel;

    private final CitaService citaService;
    private final ApplicationContext applicationContext;
    private String pacienteFiltroInicial;

    public HistorialController(CitaService citaService, ApplicationContext applicationContext) {
        this.citaService = citaService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        configurarColumnas();
        configurarFiltros();
        configurarSeleccion();
        cargarOpcionesFiltros();
        cargarHistorial();
    }

    /**
     * Permite abrir el módulo global de historial con un paciente ya seleccionado.
     * Se utiliza como acceso contextual desde el módulo Pacientes.
     */
    public void setPacienteFiltro(String nombrePaciente) {
        pacienteFiltroInicial = nombrePaciente;

        if (pacienteCombo == null) {
            return;
        }

        if (nombrePaciente == null || nombrePaciente.isBlank()) {
            pacienteCombo.setValue("Todos los pacientes");
        } else if (pacienteCombo.getItems().contains(nombrePaciente)) {
            pacienteCombo.setValue(nombrePaciente);
        } else {
            pacienteCombo.getItems().add(nombrePaciente);
            pacienteCombo.setValue(nombrePaciente);
        }

        aplicarFiltros();
    }

    private void configurarColumnas() {
        fechaColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue() != null && data.getValue().getInicio() != null
                        ? data.getValue().getInicio().format(FORMATO_FECHA)
                        : "-"
        ));

        pacienteColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue() != null ? obtenerNombrePaciente(data.getValue()) : "-"
        ));

        horarioColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue() != null && data.getValue().getInicio() != null && data.getValue().getFin() != null
                        ? data.getValue().getInicio().format(FORMATO_HORA)
                                + " - "
                                + data.getValue().getFin().format(FORMATO_HORA)
                        : "-"
        ));

        motivoColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue() != null
                        && data.getValue().getMotivo() != null
                        && !data.getValue().getMotivo().isBlank()
                        ? data.getValue().getMotivo().trim()
                        : "Sin motivo registrado"
        ));

        tratamientosColumn.setCellValueFactory(data ->
                new SimpleStringProperty(obtenerResumenTratamientos(data.getValue())));

        totalColumn.setCellValueFactory(data ->
                new SimpleStringProperty(formatearTotal(data.getValue())));

        centrarColumna(fechaColumn);
        centrarColumna(horarioColumn);
        centrarColumna(totalColumn);

        historialTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void centrarColumna(TableColumn<?, ?> columna) {
        columna.setStyle("-fx-alignment: CENTER;");
    }

    private void configurarFiltros() {
        buscarField.textProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        pacienteCombo.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        tratamientoCombo.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        fechaDesdePicker.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        fechaHastaPicker.valueProperty().addListener((obs, anterior, actual) -> aplicarFiltros());
        limpiarFiltrosButton.setOnAction(event -> limpiarFiltros());
    }

    private void configurarSeleccion() {
        btnDetalle.setDisable(true);

        historialTable.getSelectionModel().selectedItemProperty().addListener(
                (obs, anterior, actual) -> btnDetalle.setDisable(actual == null)
        );

        historialTable.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2
                    && historialTable.getSelectionModel().getSelectedItem() != null) {
                abrirDetalle();
            }
        });
    }

    private void cargarOpcionesFiltros() {
        List<Cita> historial = citaService.obtenerHistorial();

        List<String> pacientes = historial.stream()
                .map(this::obtenerNombrePaciente)
                .filter(nombre -> !nombre.isBlank())
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        List<String> tratamientos = historial.stream()
                .filter(cita -> cita.getTratamientos() != null)
                .flatMap(cita -> cita.getTratamientos().stream())
                .filter(Objects::nonNull)
                .map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .map(String::trim)
                .distinct()
                .sorted(String.CASE_INSENSITIVE_ORDER)
                .collect(Collectors.toList());

        pacienteCombo.setItems(FXCollections.observableArrayList());
        pacienteCombo.getItems().add("Todos los pacientes");
        pacienteCombo.getItems().addAll(pacientes);
        pacienteCombo.setValue("Todos los pacientes");

        tratamientoCombo.setItems(FXCollections.observableArrayList());
        tratamientoCombo.getItems().add("Todos los tratamientos");
        tratamientoCombo.getItems().addAll(tratamientos);
        tratamientoCombo.setValue("Todos los tratamientos");

        if (pacienteFiltroInicial != null && !pacienteFiltroInicial.isBlank()) {
            setPacienteFiltro(pacienteFiltroInicial);
        }
    }

    private void cargarHistorial() {
        historialTable.setItems(FXCollections.observableArrayList(citaService.obtenerHistorial()));
        aplicarFiltros();
    }

    private void aplicarFiltros() {
        if (historialTable == null) {
            return;
        }

        List<Cita> historial = citaService.obtenerHistorial();

        String busqueda = buscarField.getText() == null
                ? ""
                : buscarField.getText().trim().toLowerCase();
        String pacienteSeleccionado = pacienteCombo.getValue();
        String tratamientoSeleccionado = tratamientoCombo.getValue();
        LocalDate desde = fechaDesdePicker.getValue();
        LocalDate hasta = fechaHastaPicker.getValue();

        if (desde != null && hasta != null && hasta.isBefore(desde)) {
            vacioLabel.setText("La fecha final no puede ser anterior a la fecha inicial.");
            historialTable.setItems(FXCollections.observableArrayList());
            actualizarResumen(List.of());
            return;
        }

        List<Cita> filtradas = historial.stream()
                .filter(cita -> busqueda.isBlank() || coincideBusqueda(cita, busqueda))
                .filter(cita -> pacienteSeleccionado == null
                        || "Todos los pacientes".equals(pacienteSeleccionado)
                        || obtenerNombrePaciente(cita).equalsIgnoreCase(pacienteSeleccionado))
                .filter(cita -> tratamientoSeleccionado == null
                        || "Todos los tratamientos".equals(tratamientoSeleccionado)
                        || tieneTratamiento(cita, tratamientoSeleccionado))
                .filter(cita -> desde == null
                        || (cita.getInicio() != null && !cita.getInicio().toLocalDate().isBefore(desde)))
                .filter(cita -> hasta == null
                        || (cita.getInicio() != null && !cita.getInicio().toLocalDate().isAfter(hasta)))
                .sorted(Comparator.comparing(
                        Cita::getInicio,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .collect(Collectors.toList());

        historialTable.setItems(FXCollections.observableArrayList(filtradas));
        actualizarResumen(filtradas);
        actualizarEstadoVacio();
    }

    private boolean coincideBusqueda(Cita cita, String busqueda) {
        String paciente = obtenerNombrePaciente(cita).toLowerCase();
        String motivo = cita.getMotivo() != null ? cita.getMotivo().toLowerCase() : "";
        String tratamientos = obtenerResumenTratamientos(cita).toLowerCase();
        return paciente.contains(busqueda)
                || motivo.contains(busqueda)
                || tratamientos.contains(busqueda);
    }

    private boolean tieneTratamiento(Cita cita, String nombreTratamiento) {
        if (cita.getTratamientos() == null) {
            return false;
        }

        return cita.getTratamientos().stream()
                .filter(Objects::nonNull)
                .map(TratamientoAplicado::getNombre)
                .filter(Objects::nonNull)
                .anyMatch(nombre -> nombre.trim().equalsIgnoreCase(nombreTratamiento));
    }

    private void actualizarResumen(List<Cita> citas) {
        int consultas = citas.size();
        long tratamientos = citas.stream()
                .filter(cita -> cita.getTratamientos() != null)
                .mapToLong(cita -> cita.getTratamientos().stream().filter(Objects::nonNull).count())
                .sum();

        BigDecimal total = citas.stream()
                .map(Cita::obtenerTotalTratamientos)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        resumenLabel.setText(
                consultas + (consultas == 1 ? " consulta atendida" : " consultas atendidas")
                        + "   ·   " + tratamientos
                        + (tratamientos == 1 ? " tratamiento" : " tratamientos")
                        + "   ·   Total: $" + total.setScale(2).toPlainString()
        );
    }

    private void actualizarEstadoVacio() {
        if (historialTable.getItems().isEmpty()) {
            vacioLabel.setText("No hay consultas atendidas que coincidan con los filtros.");
        } else {
            vacioLabel.setText("");
        }
    }

    private void limpiarFiltros() {
        buscarField.clear();
        pacienteCombo.setValue("Todos los pacientes");
        tratamientoCombo.setValue("Todos los tratamientos");
        fechaDesdePicker.setValue(null);
        fechaHastaPicker.setValue(null);
    }

    private String obtenerNombrePaciente(Cita cita) {
        if (cita == null || cita.getPaciente() == null) {
            return "Paciente sin nombre";
        }

        String nombre = cita.getPaciente().getNombre() != null ? cita.getPaciente().getNombre().trim() : "";
        String paterno = cita.getPaciente().getApellidoPaterno() != null
                ? cita.getPaciente().getApellidoPaterno().trim() : "";
        String materno = cita.getPaciente().getApellidoMaterno() != null
                ? cita.getPaciente().getApellidoMaterno().trim() : "";

        return (nombre + " " + paterno + " " + materno).trim();
    }

    private String obtenerResumenTratamientos(Cita cita) {
        if (cita == null || cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) {
            return "Sin tratamientos registrados";
        }

        String resumen = cita.getTratamientos().stream()
                .filter(Objects::nonNull)
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

    @FXML
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
}
