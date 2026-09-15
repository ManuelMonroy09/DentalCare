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
    @FXML private TableView<Paciente> pacientesTable;
    @FXML private TableColumn<Paciente, Long> idColumn;
    @FXML private Button nuevoButton;
    @FXML private Button editarButton;
    @FXML private Button eliminarButton;
    @FXML private Button historialButton;
    @FXML private TextField buscarField;
    @FXML private Label contadorPacientesLabel;
    @FXML private TableColumn<Paciente, String> nombreColumn;
    @FXML private TableColumn<Paciente, String> apellidoPaternoColumn;
    @FXML private TableColumn<Paciente, String> apellidoMaternoColumn;
    @FXML private TableColumn<Paciente, String> telefonoColumn;
    @FXML private TableColumn<Paciente, String> emailColumn;
    private final PacientesService pacienteService;
    private final ApplicationContext applicationContext;
    private FilteredList<Paciente> pacientesFiltrados;

    public PacientesController(PacientesService pacienteService, ApplicationContext applicationContext) {
        this.pacienteService = pacienteService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        editarButton.setDisable(true);
        eliminarButton.setDisable(true);
        historialButton.setDisable(true);
        configurarColumnas();
        configurarSeleccion();
        configurarBusqueda();
        cargarPacientes();
        nuevoButton.setOnAction(event -> abrirFormulario());
        editarButton.setOnAction(event -> editarPaciente());
        eliminarButton.setOnAction(event -> eliminarPaciente());
        historialButton.setOnAction(event -> abrirHistorial());
    }

    private void configurarColumnas() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        apellidoPaternoColumn.setCellValueFactory(new PropertyValueFactory<>("apellidoPaterno"));
        apellidoMaternoColumn.setCellValueFactory(new PropertyValueFactory<>("apellidoMaterno"));
        telefonoColumn.setCellValueFactory(new PropertyValueFactory<>("telefono"));
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        idColumn.setPrefWidth(70);
        nombreColumn.setPrefWidth(180);
        apellidoPaternoColumn.setPrefWidth(180);
        apellidoMaternoColumn.setPrefWidth(180);
        telefonoColumn.setPrefWidth(130);
        emailColumn.setPrefWidth(220);
        pacientesTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
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
        ObservableList<Paciente> pacientes = FXCollections.observableArrayList(pacienteService.obtenerTodos());
        pacientes.sort(Comparator.comparing(Paciente::getId));
        actualizarContadorPacientes(pacientes.size());
        pacientesFiltrados = new FilteredList<>(pacientes, paciente -> true);
        pacientesTable.setItems(pacientesFiltrados);
        aplicarFiltro();
        limpiarSeleccion();
    }

    private void actualizarContadorPacientes(int total) {
        contadorPacientesLabel.setText("Total de pacientes: " + total);
    }

    private void configurarBusqueda() {
        buscarField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void aplicarFiltro() {
        if (pacientesFiltrados == null) return;
        String textoBusqueda = buscarField.getText();
        if (textoBusqueda == null || textoBusqueda.isBlank()) {
            pacientesFiltrados.setPredicate(paciente -> true);
            pacientesTable.setPlaceholder(new Label("No hay pacientes registrados."));
            return;
        }
        String[] palabras = textoBusqueda.trim().toLowerCase().split("\\s+");
        pacientesFiltrados.setPredicate(paciente -> {
            String datosPaciente = construirTextoBusqueda(paciente);
            for (String palabra : palabras) {
                if (!datosPaciente.contains(palabra)) return false;
            }
            return true;
        });
        actualizarPlaceholder();
    }

    private String construirTextoBusqueda(Paciente paciente) {
        String nombre = paciente.getNombre() != null ? paciente.getNombre().toLowerCase() : "";
        String apellidoPaterno = paciente.getApellidoPaterno() != null ? paciente.getApellidoPaterno().toLowerCase() : "";
        String apellidoMaterno = paciente.getApellidoMaterno() != null ? paciente.getApellidoMaterno().toLowerCase() : "";
        String telefono = paciente.getTelefono() != null ? paciente.getTelefono().toLowerCase() : "";
        String email = paciente.getEmail() != null ? paciente.getEmail().toLowerCase() : "";
        return nombre + " " + apellidoPaterno + " " + apellidoMaterno + " " + telefono + " " + email;
    }

    private void actualizarPlaceholder() {
        pacientesTable.setPlaceholder(new Label(
                pacientesFiltrados.isEmpty() && buscarField.getText() != null && !buscarField.getText().isBlank()
                        ? "No se encontraron pacientes."
                        : "No hay pacientes registrados."
        ));
    }

    private void configurarSeleccion() {
        pacientesTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, newValue) -> {
            boolean seleccionado = newValue != null;
            editarButton.setDisable(!seleccionado);
            eliminarButton.setDisable(!seleccionado);
            historialButton.setDisable(!seleccionado);
        });
    }

    private void limpiarSeleccion() {
        pacientesTable.getSelectionModel().clearSelection();
        editarButton.setDisable(true);
        eliminarButton.setDisable(true);
        historialButton.setDisable(true);
    }

    private void abrirFormulario() {
        limpiarSeleccion();
        abrirPacienteDialog(null);
    }

    private void editarPaciente() {
        Paciente seleccionado = pacientesTable.getSelectionModel().getSelectedItem();
        if (seleccionado != null) abrirPacienteDialog(seleccionado);
    }

    private void abrirPacienteDialog(Paciente paciente) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/PacienteDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            if (paciente != null) loader.<PacienteDialogController>getController().setPaciente(paciente);
            Stage stage = new Stage();
            stage.setTitle(paciente == null ? "Nuevo Paciente" : "Editar Paciente");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 720, 520));
            stage.setMinWidth(720);
            stage.setMinHeight(520);
            stage.setResizable(false);
            stage.showAndWait();
            cargarPacientes();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirHistorial() {
        Paciente pacienteSeleccionado = pacientesTable.getSelectionModel().getSelectedItem();
        if (pacienteSeleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/HistorialView.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            loader.<HistorialController>getController().setPacienteFiltro(obtenerNombrePaciente(pacienteSeleccionado));
            Stage stage = new Stage();
            stage.setTitle("Historial clínico");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 1200, 700));
            stage.setMinWidth(1100);
            stage.setMinHeight(650);
            stage.setResizable(true);
            stage.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String obtenerNombrePaciente(Paciente paciente) {
        return ((paciente.getNombre() == null ? "" : paciente.getNombre().trim()) + " "
                + (paciente.getApellidoPaterno() == null ? "" : paciente.getApellidoPaterno().trim()) + " "
                + (paciente.getApellidoMaterno() == null ? "" : paciente.getApellidoMaterno().trim())).trim();
    }

    private void eliminarPaciente() {
        Paciente pacienteSeleccionado = pacientesTable.getSelectionModel().getSelectedItem();
        if (pacienteSeleccionado == null) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Eliminar paciente");
        confirmacion.setHeaderText("¿Eliminar paciente?");
        confirmacion.setContentText("El paciente dejará de aparecer en el listado. Esta acción no elimina físicamente sus datos.");
        estilizarAlerta(confirmacion);

        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) return;

        try {
            pacienteService.eliminar(pacienteSeleccionado.getId());
            cargarPacientes();
        } catch (Exception e) {
            Alert error = new Alert(Alert.AlertType.ERROR);
            error.setTitle("Error");
            error.setHeaderText("No fue posible eliminar el paciente");
            error.setContentText(e.getMessage());
            estilizarAlerta(error);
            error.showAndWait();
        }
    }

    private void estilizarAlerta(Alert alerta) {
        DialogPane pane = alerta.getDialogPane();
        pane.getStyleClass().add("standard-dialog");
        String dentalcareCss = getClass().getResource("/ui/css/dentalcare.css").toExternalForm();
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        if (!pane.getStylesheets().contains(dentalcareCss)) pane.getStylesheets().add(dentalcareCss);
        if (!pane.getStylesheets().contains(dialogCss)) pane.getStylesheets().add(dialogCss);
        pane.applyCss();
        for (ButtonType tipo : pane.getButtonTypes()) {
            if (!(pane.lookupButton(tipo) instanceof Button button)) continue;
            button.getStyleClass().removeAll("dialog-primary-button", "dialog-secondary-button");
            if (tipo == ButtonType.CANCEL || tipo == ButtonType.CLOSE) {
                button.getStyleClass().add("dialog-secondary-button");
            } else {
                button.getStyleClass().add("dialog-primary-button");
            }
            button.setMinHeight(40);
            button.setPrefHeight(40);
            button.setMaxHeight(44);
            button.setWrapText(false);
            button.setMnemonicParsing(false);
        }
    }
}
