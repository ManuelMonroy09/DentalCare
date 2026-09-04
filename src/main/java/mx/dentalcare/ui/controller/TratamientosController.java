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
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.service.TratamientoService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.Locale;
import java.util.Optional;

@Component
public class TratamientosController {

    @FXML
    private TableView<Tratamiento> tratamientosTable;

    @FXML
    private TableColumn<Tratamiento, Long> idColumn;

    @FXML
    private TableColumn<Tratamiento, String> nombreColumn;

    @FXML
    private TableColumn<Tratamiento, String> descripcionColumn;

    @FXML
    private TableColumn<Tratamiento, Integer> duracionColumn;

    @FXML
    private TableColumn<Tratamiento, String> precioColumn;

    @FXML
    private TableColumn<Tratamiento, String> estadoColumn;

    @FXML
    private Button nuevoButton;

    @FXML
    private Button editarButton;

    @FXML
    private Button estadoButton;

    @FXML
    private TextField buscarField;

    @FXML
    private Label contadorTratamientosLabel;

    private final TratamientoService tratamientoService;
    private final ApplicationContext applicationContext;
    private FilteredList<Tratamiento> tratamientosFiltrados;
    private static final Locale LOCALE_MEXICO = new Locale("es", "MX");
    private static final NumberFormat FORMATO_MONEDA = NumberFormat.getCurrencyInstance(LOCALE_MEXICO);

    public TratamientosController(TratamientoService tratamientoService, ApplicationContext applicationContext) {
        this.tratamientoService = tratamientoService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        editarButton.setDisable(true);
        estadoButton.setDisable(true);
        configurarColumnas();
        configurarSeleccion();
        configurarBusqueda();
        cargarTratamientos();
        nuevoButton.setOnAction(event -> abrirFormulario());
        editarButton.setOnAction(event -> editarTratamiento());
        estadoButton.setOnAction(event -> cambiarEstado());
    }

    private void configurarColumnas() {
        idColumn.setCellValueFactory(new PropertyValueFactory<>("id"));
        nombreColumn.setCellValueFactory(new PropertyValueFactory<>("nombre"));
        descripcionColumn.setCellValueFactory(new PropertyValueFactory<>("descripcion"));
        duracionColumn.setCellValueFactory(new PropertyValueFactory<>("duracionMinutos"));
        precioColumn.setCellValueFactory(celda -> new javafx.beans.property.SimpleStringProperty(celda.getValue().getPrecio() != null ? FORMATO_MONEDA.format(celda.getValue().getPrecio()) : "$0.00"));
        estadoColumn.setCellValueFactory(celda -> new javafx.beans.property.SimpleStringProperty(celda.getValue().isActivo() ? "Activo" : "Inactivo"));
        tratamientosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        centrarColumna(idColumn);
        centrarColumna(duracionColumn);
        centrarColumna(precioColumn);
        centrarColumna(estadoColumn);
    }

    private void centrarColumna(TableColumn<?, ?> columna) {
        columna.setStyle("-fx-alignment: CENTER;");
    }

    private void cargarTratamientos() {
        ObservableList<Tratamiento> tratamientos = FXCollections.observableArrayList(tratamientoService.obtenerTodos());
        tratamientos.sort(Comparator.comparing(Tratamiento::getId, Comparator.nullsLast(Comparator.naturalOrder())));
        tratamientosFiltrados = new FilteredList<>(tratamientos, tratamiento -> true);
        tratamientosTable.setItems(tratamientosFiltrados);
        actualizarContadorTratamientos();
        aplicarFiltro();
        limpiarSeleccion();
    }

    private void actualizarContadorTratamientos() {
        int total = tratamientosFiltrados != null ? tratamientosFiltrados.size() : 0;
        contadorTratamientosLabel.setText("Tratamientos visibles: " + total);
    }

    private void configurarBusqueda() {
        buscarField.textProperty().addListener((obs, oldValue, newValue) -> aplicarFiltro());
    }

    private void aplicarFiltro() {
        if (tratamientosFiltrados == null) {
            return;
        }
        String textoBusqueda = buscarField.getText();
        if (textoBusqueda == null || textoBusqueda.isBlank()) {
            tratamientosFiltrados.setPredicate(tratamiento -> true);
            actualizarPlaceholder();
            actualizarContadorTratamientos();
            return;
        }
        String[] palabras = textoBusqueda.trim().toLowerCase(LOCALE_MEXICO).split("\\s+");
        tratamientosFiltrados.setPredicate(tratamiento -> {
            String datosTratamiento = construirTextoBusqueda(tratamiento);
            for (String palabra : palabras) {
                if (!datosTratamiento.contains(palabra)) {
                    return false;
                }
            }
            return true;
        });
        actualizarPlaceholder();
        actualizarContadorTratamientos();
    }

    private String construirTextoBusqueda(Tratamiento tratamiento) {
        String nombre = tratamiento.getNombre() != null ? tratamiento.getNombre().toLowerCase(LOCALE_MEXICO) : "";
        String descripcion = tratamiento.getDescripcion() != null ? tratamiento.getDescripcion().toLowerCase(LOCALE_MEXICO) : "";
        String precio = tratamiento.getPrecio() != null ? tratamiento.getPrecio().toPlainString() : "";
        String duracion = String.valueOf(tratamiento.getDuracionMinutos());
        String estado = tratamiento.isActivo() ? "activo" : "inactivo";
        return nombre + " " + descripcion + " " + precio + " " + duracion + " " + estado;
    }

    private void actualizarPlaceholder() {
        if (tratamientosFiltrados.isEmpty()) {
            if (buscarField.getText() == null || buscarField.getText().isBlank()) {
                tratamientosTable.setPlaceholder(new Label("No hay tratamientos registrados."));
            } else {
                tratamientosTable.setPlaceholder(new Label("No se encontraron tratamientos."));
            }

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

    private void actualizarTextoBotonEstado(
            Tratamiento tratamiento
    ) {
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
            stage.setTitle("Nuevo Tratamiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 650, 500));
            stage.setMinWidth(550);
            stage.setMinHeight(450);
            stage.showAndWait();
            cargarTratamientos();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void editarTratamiento() {
        Tratamiento tratamientoSeleccionado = tratamientosTable.getSelectionModel().getSelectedItem();
        if (tratamientoSeleccionado == null) {
            return;
        }
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/TratamientoDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            TratamientoDialogController controller = loader.getController();
            controller.setTratamiento(tratamientoSeleccionado);
            Stage stage = new Stage();
            stage.setTitle("Editar Tratamiento");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 650, 500));
            stage.setMinWidth(550);
            stage.setMinHeight(450);
            stage.showAndWait();
            cargarTratamientos();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void cambiarEstado() {
        Tratamiento tratamientoSeleccionado = tratamientosTable.getSelectionModel().getSelectedItem();
        if (tratamientoSeleccionado == null) {
            return;
        }
        boolean actualmenteActivo = tratamientoSeleccionado.isActivo();
        String accion = actualmenteActivo ? "desactivar" : "activar";
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle(actualmenteActivo ? "Desactivar tratamiento" : "Activar tratamiento");
        confirmacion.setHeaderText(actualmenteActivo ? "¿Deseas desactivar este tratamiento?" : "¿Deseas activar este tratamiento?");
        confirmacion.setContentText("Tratamiento: " + tratamientoSeleccionado.getNombre());
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) {
            return;
        }

        try {
            if (actualmenteActivo) {
                tratamientoService.desactivar(tratamientoSeleccionado.getId());
            } else {
                tratamientoService.activar(tratamientoSeleccionado.getId());
            }
            cargarTratamientos();
        } catch (Exception e) {
            mostrarError("No fue posible " + accion + " el tratamiento.", e);
        }
    }

    private void mostrarError(String mensaje, Exception e) {
        Alert alerta = new Alert(Alert.AlertType.ERROR);
        alerta.setTitle("Error");
        alerta.setHeaderText(mensaje);
        alerta.setContentText(e.getMessage() != null ? e.getMessage() : "Ocurrió un error inesperado.");
        alerta.showAndWait();
    }
}