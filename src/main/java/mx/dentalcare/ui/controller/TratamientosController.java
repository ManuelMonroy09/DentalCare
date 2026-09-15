package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mx.dentalcare.model.Tratamiento;
import mx.dentalcare.service.TratamientoService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TratamientosController {

    @FXML private TableView<Tratamiento> tratamientosTable;
    @FXML private TextField buscarField;
    @FXML private Label totalLabel;
    @FXML private Button editarButton;
    @FXML private Button estadoButton;

    private final TratamientoService tratamientoService;
    private final ApplicationContext applicationContext;

    public TratamientosController(TratamientoService tratamientoService, ApplicationContext applicationContext) {
        this.tratamientoService = tratamientoService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        cargarTratamientos();
        configurarSeleccion();
        if (buscarField != null) {
            buscarField.textProperty().addListener((obs, oldValue, newValue) -> filtrar(newValue));
        }
    }

    private void cargarTratamientos() {
        try {
            List<Tratamiento> tratamientos = tratamientoService.obtenerTodos();
            tratamientosTable.getItems().setAll(tratamientos);
            actualizarTotal();
            actualizarPlaceholder();
            limpiarSeleccion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void filtrar(String texto) {
        try {
            List<Tratamiento> tratamientos = tratamientoService.obtenerTodos();
            if (texto == null || texto.isBlank()) {
                tratamientosTable.getItems().setAll(tratamientos);
            } else {
                String filtro = texto.trim().toLowerCase();
                tratamientosTable.getItems().setAll(tratamientos.stream()
                        .filter(t -> (t.getNombre() != null && t.getNombre().toLowerCase().contains(filtro))
                                || (t.getDescripcion() != null && t.getDescripcion().toLowerCase().contains(filtro)))
                        .toList());
            }
            actualizarTotal();
            actualizarPlaceholder();
            limpiarSeleccion();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void actualizarTotal() {
        if (totalLabel != null) {
            totalLabel.setText(String.valueOf(tratamientosTable.getItems().size()));
        }
    }

    private void actualizarPlaceholder() {
        if (tratamientosTable.getItems().isEmpty()) {
            tratamientosTable.setPlaceholder(new Label(
                    buscarField.getText() == null || buscarField.getText().isBlank()
                            ? "No hay tratamientos registrados."
                            : "No se encontraron tratamientos."));
        } else {
            tratamientosTable.setPlaceholder(new Label("No hay tratamientos registrados."));
        }
    }

    private void configurarSeleccion() {
        tratamientosTable.getSelectionModel().selectedItemProperty().addListener((obs, oldValue, nuevoValor) -> {
            boolean seleccionado = nuevoValor != null;
            editarButton.setDisable(!seleccionado);
            estadoButton.setDisable(!seleccionado);
            actualizarTextoBotonEstado(nuevoValor);
        });
    }

    private void actualizarTextoBotonEstado(Tratamiento tratamiento) {
        if (tratamiento == null) {
            estadoButton.setText("Desactivar");
            return;
        }
        estadoButton.setText(tratamiento.isActivo() ? "Desactivar" : "Activar");
    }

    private void limpiarSeleccion() {
        tratamientosTable.getSelectionModel().clearSelection();
        editarButton.setDisable(true);
        estadoButton.setDisable(true);
        estadoButton.setText("Desactivar");
    }

    private void abrirFormulario() {
        limpiarSeleccion();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/TratamientoDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            Stage stage = new Stage();
            stage.setTitle("Nuevo tratamiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 620, 430));
            stage.setMinWidth(620);
            stage.setMinHeight(430);
            stage.setResizable(false);
            stage.showAndWait();
            cargarTratamientos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editarTratamiento() {
        Tratamiento tratamientoSeleccionado = tratamientosTable.getSelectionModel().getSelectedItem();
        if (tratamientoSeleccionado == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/TratamientoDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            TratamientoDialogController controller = loader.getController();
            controller.setTratamiento(tratamientoSeleccionado);
            Stage stage = new Stage();
            stage.setTitle("Editar tratamiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 620, 430));
            stage.setMinWidth(620);
            stage.setMinHeight(430);
            stage.setResizable(false);
            stage.showAndWait();
            cargarTratamientos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cambiarEstado() {
        Tratamiento tratamientoSeleccionado = tratamientosTable.getSelectionModel().getSelectedItem();
        if (tratamientoSeleccionado == null) return;
        try {
            tratamientoService.cambiarEstado(tratamientoSeleccionado);
            cargarTratamientos();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
