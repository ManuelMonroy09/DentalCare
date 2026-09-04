package mx.dentalcare.ui.controller;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.service.PacientesService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.Optional;

@Component
public class PacientesController {

    @FXML
    private TableView<Paciente> pacientesTable;

    @FXML
    private TableColumn<Paciente, Long> idColumn;

    @FXML
    private Button nuevoButton;

    @FXML
    private Button editarButton;

    @FXML
    private Button eliminarButton;

    @FXML
    private TextField buscarField;

    @FXML
    private Label contadorPacientesLabel;

    @FXML
    private TableColumn<Paciente, String> nombreColumn;

    @FXML
    private TableColumn<Paciente, String> apellidoPaternoColumn;

    @FXML
    private TableColumn<Paciente, String> apellidoMaternoColumn;

    @FXML
    private TableColumn<Paciente, String> telefonoColumn;

    @FXML
    private TableColumn<Paciente, String> emailColumn;

    private final PacientesService pacienteService;
    private final ApplicationContext applicationContext;

    private FilteredList<Paciente> pacientesFiltrados;

    public PacientesController(
            PacientesService pacienteService,
            ApplicationContext applicationContext) {

        this.pacienteService = pacienteService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {

        editarButton.setDisable(true);
        eliminarButton.setDisable(true);

        configurarColumnas();
        configurarSeleccion();
        configurarBusqueda();
        cargarPacientes();

        nuevoButton.setOnAction(event -> abrirFormulario());
        editarButton.setOnAction(event -> editarPaciente());
        eliminarButton.setOnAction(event -> eliminarPaciente());
    }

    private void configurarColumnas() {

        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        apellidoPaternoColumn.setCellValueFactory(
                new PropertyValueFactory<>("apellidoPaterno")
        );
        apellidoMaternoColumn.setCellValueFactory(
                new PropertyValueFactory<>("apellidoMaterno")
        );
        telefonoColumn.setCellValueFactory(
                new PropertyValueFactory<>("telefono")
        );
        emailColumn.setCellValueFactory(
                new PropertyValueFactory<>("email")
        );

        idColumn.setPrefWidth(70);
        nombreColumn.setPrefWidth(180);
        apellidoPaternoColumn.setPrefWidth(180);
        apellidoMaternoColumn.setPrefWidth(180);
        telefonoColumn.setPrefWidth(130);
        emailColumn.setPrefWidth(220);

        pacientesTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY
        );

        centrarColumna(idColumn);
        centrarColumna(nombreColumn);
        centrarColumna(apellidoPaternoColumn);
        centrarColumna(apellidoMaternoColumn);
        centrarColumna(telefonoColumn);
        centrarColumna(emailColumn);
    }

    private void centrarColumna(TableColumn<?, ?> columna) {

        columna.setStyle("-fx-alignment: CENTER;");
    }

    private void cargarPacientes() {

        ObservableList<Paciente> pacientes =
                FXCollections.observableArrayList(
                        pacienteService.obtenerTodos()
                );
        pacientes.sort(Comparator.comparing(Paciente::getId));
        actualizarContadorPacientes(pacientes.size());
        pacientesFiltrados = new FilteredList<>(
                pacientes,
                paciente -> true
        );

        pacientesTable.setItems(pacientesFiltrados);
        aplicarFiltro();
        limpiarSeleccion();
    }

    private void actualizarContadorPacientes(int total) {
        contadorPacientesLabel.setText(
                "Total de pacientes: " + total
        );
    }

    private void configurarBusqueda() {

        buscarField.textProperty().addListener(
                (obs, oldValue, newValue) -> aplicarFiltro()
        );
    }

    private void aplicarFiltro() {

        if (pacientesFiltrados == null) {
            return;
        }

        String textoBusqueda = buscarField.getText();

        if (textoBusqueda == null || textoBusqueda.isBlank()) {

            pacientesFiltrados.setPredicate(paciente -> true);

            pacientesTable.setPlaceholder(
                    new Label("No hay pacientes registrados.")
            );

            return;
        }

        String[] palabras = textoBusqueda
                .trim()
                .toLowerCase()
                .split("\\s+");

        pacientesFiltrados.setPredicate(paciente -> {

            String datosPaciente = construirTextoBusqueda(paciente);

            for (String palabra : palabras) {

                if (!datosPaciente.contains(palabra)) {
                    return false;
                }
            }

            return true;
        });

        actualizarPlaceholder();
    }

    private String construirTextoBusqueda(Paciente paciente) {

        String nombre = paciente.getNombre() != null
                ? paciente.getNombre().toLowerCase()
                : "";

        String apellidoPaterno = paciente.getApellidoPaterno() != null
                ? paciente.getApellidoPaterno().toLowerCase()
                : "";

        String apellidoMaterno = paciente.getApellidoMaterno() != null
                ? paciente.getApellidoMaterno().toLowerCase()
                : "";

        String telefono = paciente.getTelefono() != null
                ? paciente.getTelefono().toLowerCase()
                : "";

        String email = paciente.getEmail() != null
                ? paciente.getEmail().toLowerCase()
                : "";

        return nombre + " "
                + apellidoPaterno + " "
                + apellidoMaterno + " "
                + telefono + " "
                + email;
    }

    private void actualizarPlaceholder() {

        if (pacientesFiltrados.isEmpty()) {

            if (buscarField.getText() == null
                    || buscarField.getText().isBlank()) {

                pacientesTable.setPlaceholder(
                        new Label("No hay pacientes registrados.")
                );

            } else {

                pacientesTable.setPlaceholder(
                        new Label("No se encontraron pacientes.")
                );
            }

        } else {

            pacientesTable.setPlaceholder(
                    new Label("No hay pacientes registrados.")
            );
        }
    }

    private void configurarSeleccion() {

        pacientesTable.getSelectionModel()
                .selectedItemProperty()
                .addListener((obs, oldValue, newValue) -> {

                    boolean seleccionado = newValue != null;

                    editarButton.setDisable(!seleccionado);
                    eliminarButton.setDisable(!seleccionado);
                });
    }

    private void limpiarSeleccion() {

        pacientesTable.getSelectionModel().clearSelection();

        editarButton.setDisable(true);
        eliminarButton.setDisable(true);
    }

    private void abrirFormulario() {

        limpiarSeleccion();

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/ui/fxml/PacienteDialog.fxml"
                    )
            );

            loader.setControllerFactory(
                    applicationContext::getBean
            );

            Parent root = loader.load();

            Stage stage = new Stage();

            stage.setTitle("Nuevo Paciente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 650, 500));
            stage.setMinWidth(550);
            stage.setMinHeight(450);

            stage.showAndWait();

            cargarPacientes();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void editarPaciente() {

        Paciente pacienteSeleccionado =
                pacientesTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (pacienteSeleccionado == null) {
            return;
        }

        try {

            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource(
                            "/ui/fxml/PacienteDialog.fxml"
                    )
            );

            loader.setControllerFactory(
                    applicationContext::getBean
            );

            Parent root = loader.load();

            PacienteDialogController controller =
                    loader.getController();

            controller.setPaciente(pacienteSeleccionado);

            Stage stage = new Stage();

            stage.setTitle("Editar Paciente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 650, 500));
            stage.setMinWidth(550);
            stage.setMinHeight(450);

            stage.showAndWait();

            cargarPacientes();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    private void eliminarPaciente() {

        Paciente pacienteSeleccionado =
                pacientesTable
                        .getSelectionModel()
                        .getSelectedItem();

        if (pacienteSeleccionado == null) {
            return;
        }

        Alert confirmacion =
                new Alert(Alert.AlertType.CONFIRMATION);

        confirmacion.setTitle("Eliminar paciente");
        confirmacion.setHeaderText(
                "¿Deseas eliminar este paciente?"
        );

        confirmacion.setContentText(
                "ID: " + pacienteSeleccionado.getId()
                        + "\nNombre: "
                        + pacienteSeleccionado.getNombre()
                        + " "
                        + pacienteSeleccionado.getApellidoPaterno()
        );

        Optional<ButtonType> resultado =
                confirmacion.showAndWait();

        if (resultado.isPresent()
                && resultado.get() == ButtonType.OK) {

            pacienteService.eliminar(
                    pacienteSeleccionado.getId()
            );

            cargarPacientes();
        }
    }
}