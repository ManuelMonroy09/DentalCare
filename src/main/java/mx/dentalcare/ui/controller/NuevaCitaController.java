package mx.dentalcare.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.Stage;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.PacientesService;
import mx.dentalcare.service.TratamientoService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
public class NuevaCitaController {
    @FXML private ComboBox<Paciente> cmbPaciente;
    @FXML private DatePicker dateFecha;
    @FXML private ComboBox<String> cmbHora;
    @FXML private ComboBox<Integer> cmbDuracion;
    @FXML private TextArea txtMotivo;
    @FXML private ComboBox<Tratamiento> cmbTratamiento;
    @FXML private Button btnAgregarTratamiento;
    @FXML private ListView<TratamientoAplicado> lstTratamientos;
    @FXML private Label lblTotalTratamientos;
    @FXML private TextArea txtNotas;
    @FXML private Label lblError;
    @FXML private Button btnCancelar;
    @FXML private Button btnGuardar;

    private final PacientesService pacientesService;
    private final CitaService citaService;
    private final TratamientoService tratamientoService;
    private Cita citaEditar;
    private boolean modoEdicion;
    private final ObservableList<TratamientoAplicado> tratamientosSeleccionados = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public NuevaCitaController(PacientesService pacientesService, CitaService citaService, TratamientoService tratamientoService) {
        this.pacientesService = pacientesService;
        this.citaService = citaService;
        this.tratamientoService = tratamientoService;
    }

    @FXML
    public void initialize() {
        configurarPacientes();
        configurarHoras();
        configurarDuraciones();
        configurarTratamientos();
        configurarListaTratamientos();
        configurarEventos();
        dateFecha.setValue(LocalDate.now());
        cmbDuracion.getSelectionModel().select(Integer.valueOf(60));
        actualizarTotalTratamientos();
    }

    private void configurarPacientes() {
        List<Paciente> pacientes = pacientesService.obtenerTodos();
        pacientes.sort(Comparator.comparing(Paciente::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        cmbPaciente.getItems().setAll(pacientes);
        cmbPaciente.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Paciente paciente, boolean empty) {
                super.updateItem(paciente, empty);
                setText(empty || paciente == null ? null : formatearPaciente(paciente));
            }
        });
        cmbPaciente.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Paciente paciente, boolean empty) {
                super.updateItem(paciente, empty);
                setText(empty || paciente == null ? null : formatearPaciente(paciente));
            }
        });
    }

    private String formatearPaciente(Paciente paciente) {
        String nombre = paciente.getNombre() != null ? paciente.getNombre().trim() : "";
        String apellidoPaterno = paciente.getApellidoPaterno() != null ? paciente.getApellidoPaterno().trim() : "";
        String apellidoMaterno = paciente.getApellidoMaterno() != null ? paciente.getApellidoMaterno().trim() : "";
        return (nombre + " " + apellidoPaterno + " " + apellidoMaterno).trim().replaceAll("\\s+", " ");
    }

    private void configurarHoras() {
        cmbHora.getItems().clear();
        for (int hora = 8; hora <= 19; hora++) {
            agregarHora(hora, 0);
            agregarHora(hora, 30);
        }
        cmbHora.getSelectionModel().select("08:00");
    }

    private void agregarHora(int hora, int minutos) {
        cmbHora.getItems().add(String.format("%02d:%02d", hora, minutos));
    }

    private void configurarDuraciones() {
        cmbDuracion.getItems().setAll(30, 45, 60, 90, 120, 150, 180);
    }

    private void configurarTratamientos() {
        List<Tratamiento> tratamientos = new ArrayList<>(tratamientoService.obtenerActivos());
        tratamientos.sort(Comparator.comparing(Tratamiento::getNombre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
        cmbTratamiento.getItems().setAll(tratamientos);
        cmbTratamiento.setCellFactory(listView -> new ListCell<>() {
            @Override protected void updateItem(Tratamiento tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);
                setText(empty || tratamiento == null ? null : formatearTratamiento(tratamiento));
            }
        });
        cmbTratamiento.setButtonCell(new ListCell<>() {
            @Override protected void updateItem(Tratamiento tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);
                setText(empty || tratamiento == null ? null : formatearTratamiento(tratamiento));
            }
        });
    }

    private String formatearTratamiento(Tratamiento tratamiento) {
        if (tratamiento == null) return "";
        BigDecimal precio = tratamiento.getPrecio() != null ? tratamiento.getPrecio() : BigDecimal.ZERO;
        return tratamiento.getNombre() + "  ·  $" + precio.setScale(2).toPlainString();
    }

    private void configurarListaTratamientos() {
        lstTratamientos.setItems(tratamientosSeleccionados);
        lstTratamientos.setCellFactory(listView -> new ListCell<>() {
            private final Label lblNombre = new Label();
            private final Label lblPrecio = new Label();
            private final Button btnEliminar = new Button("×");
            private final HBox contenedor = new HBox(10, lblNombre, lblPrecio, btnEliminar);
            {
                HBox.setHgrow(lblNombre, Priority.ALWAYS);
                btnEliminar.setFocusTraversable(false);
                btnEliminar.getStyleClass().add("danger-button");
                contenedor.setMaxWidth(Double.MAX_VALUE);
                contenedor.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                btnEliminar.setOnAction(event -> {
                    TratamientoAplicado tratamiento = getItem();
                    if (tratamiento != null) {
                        tratamientosSeleccionados.remove(tratamiento);
                        actualizarTotalTratamientos();
                    }
                });
            }
            @Override protected void updateItem(TratamientoAplicado tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);
                if (empty || tratamiento == null) {
                    setGraphic(null);
                    return;
                }
                lblNombre.setText(tratamiento.getNombre());
                BigDecimal precio = tratamiento.getPrecio() != null ? tratamiento.getPrecio() : BigDecimal.ZERO;
                lblPrecio.setText("$" + precio.setScale(2).toPlainString());
                setGraphic(contenedor);
            }
        });
    }

    private void configurarEventos() {
        btnAgregarTratamiento.setOnAction(event -> agregarTratamiento());
        btnCancelar.setOnAction(event -> cerrarVentana());
        btnGuardar.setOnAction(event -> guardarCita());
    }

    private void agregarTratamiento() {
        Tratamiento tratamiento = cmbTratamiento.getValue();
        if (tratamiento == null) return;
        boolean yaAgregado = tratamientosSeleccionados.stream()
                .anyMatch(item -> item.getTratamientoId() != null && item.getTratamientoId().equals(tratamiento.getId()));
        if (yaAgregado) return;
        TratamientoAplicado aplicado = new TratamientoAplicado(
                tratamiento.getId(), tratamiento.getNombre(),
                tratamiento.getPrecio() != null ? tratamiento.getPrecio() : BigDecimal.ZERO,
                tratamiento.getDuracionMinutos());
        tratamientosSeleccionados.add(aplicado);
        cmbTratamiento.getSelectionModel().clearSelection();
        actualizarTotalTratamientos();
    }

    private void actualizarTotalTratamientos() {
        BigDecimal total = tratamientosSeleccionados.stream()
                .map(TratamientoAplicado::getPrecio)
                .filter(precio -> precio != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        lblTotalTratamientos.setText("$" + total.setScale(2).toPlainString());
    }

    private void guardarCita() {
        lblError.setVisible(false);
        lblError.setManaged(false);
        if (cmbPaciente.getValue() == null) { mostrarError("Selecciona un paciente."); return; }
        if (dateFecha.getValue() == null) { mostrarError("Selecciona una fecha."); return; }
        if (cmbHora.getValue() == null || cmbHora.getValue().isBlank()) { mostrarError("Selecciona una hora de inicio."); return; }
        if (cmbDuracion.getValue() == null) { mostrarError("Selecciona una duración."); return; }
        try {
            LocalTime hora = LocalTime.parse(cmbHora.getValue(), FORMATO_HORA);
            LocalDateTime inicio = LocalDateTime.of(dateFecha.getValue(), hora);
            int duracion = cmbDuracion.getValue();
            LocalDateTime fin = inicio.plusMinutes(duracion);
            Cita cita;
            if (modoEdicion && citaEditar != null) {
                cita = citaEditar;
                cita.setPaciente(cmbPaciente.getValue());
                cita.setInicio(inicio);
                cita.setFin(fin);
            } else {
                cita = new Cita(cmbPaciente.getValue(), inicio, fin);
            }
            cita.setMotivo(txtMotivo.getText());
            cita.setNotas(txtNotas.getText());
            cita.setTratamientos(new ArrayList<>(tratamientosSeleccionados));
            citaService.guardar(cita);
            cerrarVentana();
        } catch (Exception e) {
            mostrarError("No fue posible guardar la cita: " + e.getMessage());
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    public void prepararNuevaCita(LocalDate fecha, String hora) {
        modoEdicion = false;
        citaEditar = null;
        dateFecha.setValue(fecha != null ? fecha : LocalDate.now());
        if (hora != null && cmbHora.getItems().contains(hora)) cmbHora.getSelectionModel().select(hora);
        cmbPaciente.getSelectionModel().clearSelection();
        txtMotivo.clear();
        txtNotas.clear();
        tratamientosSeleccionados.clear();
        actualizarTotalTratamientos();
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void prepararNuevaCita(LocalDate fecha, LocalTime hora) {
        prepararNuevaCita(fecha, hora != null ? hora.format(FORMATO_HORA) : null);
    }

    public void prepararEdicion(Cita cita) {
        modoEdicion = true;
        citaEditar = cita;
        if (cita == null) return;
        cmbPaciente.setValue(cita.getPaciente());
        if (cita.getInicio() != null) {
            dateFecha.setValue(cita.getInicio().toLocalDate());
            String hora = cita.getInicio().format(FORMATO_HORA);
            if (cmbHora.getItems().contains(hora)) cmbHora.getSelectionModel().select(hora);
        }
        if (cita.getInicio() != null && cita.getFin() != null) {
            int duracion = (int) java.time.Duration.between(cita.getInicio(), cita.getFin()).toMinutes();
            if (cmbDuracion.getItems().contains(duracion)) cmbDuracion.getSelectionModel().select(Integer.valueOf(duracion));
        }
        txtMotivo.setText(cita.getMotivo());
        txtNotas.setText(cita.getNotas());
        tratamientosSeleccionados.setAll(cita.getTratamientos() != null ? cita.getTratamientos() : List.of());
        actualizarTotalTratamientos();
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    public void prepararParaEdicion(Cita cita) {
        prepararEdicion(cita);
    }

    public void setCitaEditar(Cita cita) {
        prepararEdicion(cita);
    }
}