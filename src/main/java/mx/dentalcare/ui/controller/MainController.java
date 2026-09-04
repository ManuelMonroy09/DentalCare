package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button inicioButton;
    @FXML private Button pacientesButton;
    @FXML private Button citasButton;
    @FXML private Button tratamientosButton;
    @FXML private Button historialButton;
    @FXML private Button finanzasButton;
    @FXML private Button configuracionButton;

    private final ApplicationContext context;

    public MainController(ApplicationContext context) { this.context = context; }

    @FXML public void initialize() { mostrarInicio(); }

    @FXML private void mostrarInicio() { cargarVista("/ui/fxml/DashboardView.fxml"); }
    @FXML private void mostrarPacientes() { cargarVista("/ui/fxml/PacientesView.fxml"); }
    @FXML private void mostrarCitas() { cargarVista("/ui/fxml/AgendaView.fxml"); }
    @FXML private void mostrarTratamientos() { cargarVista("/ui/fxml/TratamientosView.fxml"); }
    @FXML private void mostrarHistorial() { cargarVista("/ui/fxml/HistorialView.fxml"); }
    @FXML private void mostrarFinanzas() { cargarVista("/ui/fxml/FinanzasView.fxml"); }
    @FXML private void mostrarConfiguracion() { mostrarMensaje("Configuración", "Módulo de configuración próximamente."); }

    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            loader.setControllerFactory(context::getBean);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("No fue posible cargar la vista: " + ruta, e);
        }
    }

    private void mostrarMensaje(String titulo, String mensaje) {
        Label title = new Label(titulo);
        title.getStyleClass().add("page-title");
        Label description = new Label(mensaje);
        description.getStyleClass().add("page-subtitle");
        javafx.scene.layout.VBox box = new javafx.scene.layout.VBox(10, title, description);
        box.getStyleClass().add("content");
        contentArea.getChildren().setAll(box);
    }
}
