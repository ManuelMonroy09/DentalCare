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
import javafx.scene.control.TextField;
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

    @FXML
    private ComboBox<Paciente> cmbPaciente;

    @FXML
    private DatePicker dateFecha;

    @FXML
    private ComboBox<String> cmbHora;

    @FXML
    private ComboBox<Integer> cmbDuracion;

    @FXML
    private TextField txtMotivo;

    @FXML
    private ComboBox<Tratamiento> cmbTratamiento;

    @FXML
    private Button btnAgregarTratamiento;

    @FXML
    private ListView<TratamientoAplicado> lstTratamientos;

    @FXML
    private Label lblTotalTratamientos;

    @FXML
    private TextArea txtNotas;

    @FXML
    private Label lblError;

    @FXML
    private Button btnCancelar;

    @FXML
    private Button btnGuardar;

    private final PacientesService pacientesService;
    private final CitaService citaService;
    private final TratamientoService tratamientoService;
    private Cita citaEditar;
    private boolean modoEdicion;
    private final ObservableList<TratamientoAplicado> tratamientosSeleccionados = FXCollections.observableArrayList();
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public NuevaCitaController(PacientesService pacientesService,
                               CitaService citaService,
                               TratamientoService tratamientoService) {
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
        pacientes.sort(Comparator.comparing(
                Paciente::getNombre,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));

        cmbPaciente.getItems().setAll(pacientes);

        cmbPaciente.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Paciente paciente, boolean empty) {
                super.updateItem(paciente, empty);
                setText(empty || paciente == null ? null : formatearPaciente(paciente));
            }
        });

        cmbPaciente.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Paciente paciente, boolean empty) {
                super.updateItem(paciente, empty);
                setText(empty || paciente == null ? null : formatearPaciente(paciente));
            }
        });
    }

    private String formatearPaciente(Paciente paciente) {
        String nombre = paciente.getNombre() != null ? paciente.getNombre().trim() : "";
        String apellidoPaterno = paciente.getApellidoPaterno() != null ? paciente.getApellidoPaterno().trim() : "";
        String apellidoMaterno = paciente.getApellidoMaterno() != null ? paciente.getApellidoMaterno().trim() : "";

        return (nombre + " " + apellidoPaterno + " " + apellidoMaterno)
                .trim()
                .replaceAll("\\s+", " ");
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
        tratamientos.sort(Comparator.comparing(
                Tratamiento::getNombre,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)
        ));
        cmbTratamiento.getItems().setAll(tratamientos);

        cmbTratamiento.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(Tratamiento tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);
                setText(empty || tratamiento == null ? null : formatearTratamiento(tratamiento));
            }
        });

        cmbTratamiento.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(Tratamiento tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);
                setText(empty || tratamiento == null ? null : formatearTratamiento(tratamiento));
            }
        });
    }

    private String formatearTratamiento(Tratamiento tratamiento) {
        if (tratamiento == null) {
            return "";
        }

        BigDecimal precio = tratamiento.getPrecio() != null
                ? tratamiento.getPrecio()
                : BigDecimal.ZERO;

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

            @Override
            protected void updateItem(TratamientoAplicado tratamiento, boolean empty) {
                super.updateItem(tratamiento, empty);

                if (empty || tratamiento == null) {
                    setGraphic(null);
                    return;
                }

                lblNombre.setText(tratamiento.getNombre());
                BigDecimal precio = tratamiento.getPrecio() != null
                        ? tratamiento.getPrecio()
                        : BigDecimal.ZERO;
                lblPrecio.setText("$" + precio.setScale(2).toPlainString());
                setGraphic(contenedor);
            }
        });
    }

    private void agregarTratamientoSeleccionado() {
        limpiarError();

        Tratamiento tratamiento = cmbTratamiento.getSelectionModel().getSelectedItem();

        if (tratamiento == null) {
            mostrarError("Selecciona un tratamiento.");
            return;
        }

        if (!tratamiento.isActivo()) {
            mostrarError("No se puede agregar un tratamiento inactivo.");
            return;
        }

        boolean yaSeleccionado = tratamientosSeleccionados.stream()
                .anyMatch(aplicado -> aplicado != null
                        && tratamiento.getId() != null
                        && tratamiento.getId().equals(aplicado.getTratamientoId()));

        if (yaSeleccionado) {
            mostrarError("Ese tratamiento ya está agregado a la cita.");
            return;
        }

        tratamientosSeleccionados.add(new TratamientoAplicado(
                tratamiento.getId(),
                tratamiento.getNombre(),
                tratamiento.getPrecio(),
                tratamiento.getDuracionMinutos()
        ));

        cmbTratamiento.getSelectionModel().clearSelection();
        actualizarTotalTratamientos();
    }

    private void actualizarTotalTratamientos() {
        BigDecimal total = tratamientosSeleccionados.stream()
                .map(TratamientoAplicado::obtenerImporte)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        lblTotalTratamientos.setText("$" + total.setScale(2).toPlainString());
    }

    private void configurarEventos() {
        btnGuardar.setOnAction(event -> guardarCita());
        btnCancelar.setOnAction(event -> cerrarVentana());
        btnAgregarTratamiento.setOnAction(event -> agregarTratamientoSeleccionado());
    }

    public void prepararParaEdicion(Cita cita) {
        if (cita == null) {
            return;
        }

        this.citaEditar = cita;
        this.modoEdicion = true;
        cargarDatosCita();
        btnGuardar.setText("Guardar cambios");
    }

    private void cargarDatosCita() {
        if (citaEditar == null) {
            return;
        }

        if (citaEditar.getPaciente() != null && citaEditar.getPaciente().getId() != null) {
            Long pacienteId = citaEditar.getPaciente().getId();
            cmbPaciente.getItems().stream()
                    .filter(paciente -> paciente.getId() != null && paciente.getId().equals(pacienteId))
                    .findFirst()
                    .ifPresent(paciente -> cmbPaciente.getSelectionModel().select(paciente));
        }

        if (citaEditar.getInicio() != null) {
            dateFecha.setValue(citaEditar.getInicio().toLocalDate());
            cmbHora.getSelectionModel().select(citaEditar.getInicio().toLocalTime().format(FORMATO_HORA));
        }

        int duracion = (int) citaEditar.getDuracionMinutos();
        if (!cmbDuracion.getItems().contains(duracion)) {
            cmbDuracion.getItems().add(duracion);
        }
        cmbDuracion.getSelectionModel().select(duracion);

        txtMotivo.setText(citaEditar.getMotivo() != null ? citaEditar.getMotivo() : "");
        txtNotas.setText(citaEditar.getNotas() != null ? citaEditar.getNotas() : "");

        tratamientosSeleccionados.clear();
        if (citaEditar.getTratamientos() != null) {
            tratamientosSeleccionados.addAll(citaEditar.getTratamientos());
        }
        actualizarTotalTratamientos();
    }

    public void prepararNuevaCita(LocalDate fecha, LocalTime hora) {
        if (fecha != null) {
            dateFecha.setValue(fecha);
        }

        if (hora != null) {
            String horaFormateada = hora.format(FORMATO_HORA);
            if (cmbHora.getItems().contains(horaFormateada)) {
                cmbHora.getSelectionModel().select(horaFormateada);
            }
        }

        modoEdicion = false;
        citaEditar = null;
        txtMotivo.clear();
        txtNotas.clear();
        tratamientosSeleccionados.clear();
        actualizarTotalTratamientos();
        btnGuardar.setText("Guardar cita");
    }

    private void guardarCita() {
        limpiarError();

        try {
            Paciente paciente = cmbPaciente.getSelectionModel().getSelectedItem();
            if (paciente == null) {
                mostrarError("Selecciona un paciente.");
                return;
            }

            LocalDate fecha = dateFecha.getValue();
            if (fecha == null) {
                mostrarError("Selecciona una fecha para la cita.");
                return;
            }

            String horaSeleccionada = cmbHora.getSelectionModel().getSelectedItem();
            if (horaSeleccionada == null || horaSeleccionada.isBlank()) {
                mostrarError("Selecciona una hora de inicio.");
                return;
            }

            Integer duracion = cmbDuracion.getSelectionModel().getSelectedItem();
            if (duracion == null || duracion <= 0) {
                mostrarError("Selecciona una duración válida.");
                return;
            }

            LocalDateTime inicio = LocalDateTime.of(fecha, LocalTime.parse(horaSeleccionada));
            String motivo = obtenerMotivo();
            String notas = txtNotas.getText();
            List<Long> tratamientoIds = obtenerTratamientoIdsSeleccionados();

            if (modoEdicion) {
                citaEditar.setPaciente(paciente);
                citaEditar.setInicio(inicio);
                citaEditar.establecerDuracion(duracion);
                citaEditar.setMotivo(motivo);
                citaEditar.setNotas(limpiarTexto(notas));
                citaService.guardarConTratamientos(citaEditar, tratamientoIds);
            } else {
                Cita cita = citaService.crear(paciente, inicio, duracion);
                cita.setMotivo(motivo);
                cita.setNotas(limpiarTexto(notas));
                citaService.guardarConTratamientos(cita, tratamientoIds);
            }

            cerrarVentana();

        } catch (IllegalStateException | IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            mostrarError("No fue posible guardar la cita. Revisa los datos e inténtalo nuevamente.");
        }
    }

    private List<Long> obtenerTratamientoIdsSeleccionados() {
        Set<Long> ids = new HashSet<>();

        for (TratamientoAplicado tratamiento : tratamientosSeleccionados) {
            if (tratamiento != null && tratamiento.getTratamientoId() != null) {
                ids.add(tratamiento.getTratamientoId());
            }
        }

        return new ArrayList<>(ids);
    }

    private String obtenerMotivo() {
        return limpiarTexto(txtMotivo.getText());
    }

    private String limpiarTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        return texto.trim();
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje != null ? mensaje : "Ocurrió un error inesperado.");
        lblError.setVisible(true);
        lblError.setManaged(true);
    }

    private void limpiarError() {
        lblError.setText("");
        lblError.setVisible(false);
        lblError.setManaged(false);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCancelar.getScene().getWindow();
        stage.close();
    }

    public void setCitaEditar(Cita citaEditar) {
        if (citaEditar == null) {
            return;
        }

        this.citaEditar = citaEditar;
        this.modoEdicion = true;

        if (cmbPaciente != null) {
            cargarDatosCita();
        }
    }
}
