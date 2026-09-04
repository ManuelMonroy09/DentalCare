package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextField;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import mx.dentalcare.domain.configuracion.ConfiguracionConsultorio;
import mx.dentalcare.service.BackupService;
import mx.dentalcare.service.ConfiguracionService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
public class ConfiguracionController {
    @FXML private TextField txtNombreConsultorio;
    @FXML private TextField txtOdontologo;
    @FXML private TextField txtTelefono;
    @FXML private TextField txtEmail;
    @FXML private TextField txtDireccion;
    @FXML private TextField txtPieRecibo;

    private final BackupService backupService;
    private final ConfiguracionService configuracionService;

    public ConfiguracionController(BackupService backupService, ConfiguracionService configuracionService) {
        this.backupService = backupService;
        this.configuracionService = configuracionService;
    }

    @FXML
    public void initialize() {
        cargarConfiguracion();
    }

    @FXML
    private void guardarConfiguracion() {
        try {
            ConfiguracionConsultorio configuracion = new ConfiguracionConsultorio();
            configuracion.setNombreConsultorio(txtNombreConsultorio.getText());
            configuracion.setNombreOdontologo(txtOdontologo.getText());
            configuracion.setTelefono(txtTelefono.getText());
            configuracion.setEmail(txtEmail.getText());
            configuracion.setDireccion(txtDireccion.getText());
            configuracion.setPieRecibo(txtPieRecibo.getText());
            configuracionService.guardar(configuracion);
            mostrarInformacion("Configuración guardada", "Los datos del consultorio se guardaron correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError("No fue posible guardar la configuración", ex.getMessage());
        }
    }

    private void cargarConfiguracion() {
        ConfiguracionConsultorio c = configuracionService.obtener();
        txtNombreConsultorio.setText(c.getNombreConsultorio());
        txtOdontologo.setText(c.getNombreOdontologo());
        txtTelefono.setText(c.getTelefono());
        txtEmail.setText(c.getEmail());
        txtDireccion.setText(c.getDireccion());
        txtPieRecibo.setText(c.getPieRecibo());
    }

    @FXML
    private void crearRespaldo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Guardar respaldo de DentalCare");
        chooser.setInitialFileName("DentalCare_backup.zip");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo DentalCare (*.zip)", "*.zip"));
        java.io.File archivo = chooser.showSaveDialog(null);
        if (archivo == null) return;
        try {
            Path respaldo = backupService.crearRespaldo(archivo.toPath());
            mostrarInformacion("Respaldo creado", "El respaldo se creó correctamente en:\n" + respaldo);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError("No fue posible crear el respaldo", ex.getMessage());
        }
    }

    @FXML
    private void restaurarRespaldo() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Seleccionar respaldo de DentalCare");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Respaldo DentalCare (*.zip)", "*.zip"));
        java.io.File archivo = chooser.showOpenDialog(null);
        if (archivo == null) return;

        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Restaurar respaldo");
        confirmacion.setHeaderText("¿Restaurar este respaldo?");
        confirmacion.setContentText("Los datos actuales serán reemplazados por el contenido del respaldo. Se recomienda reiniciar DentalCare después de restaurar.");
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty() || resultado.get() != ButtonType.OK) return;

        try {
            backupService.restaurarRespaldo(archivo.toPath());
            mostrarInformacion("Respaldo restaurado", "Los datos fueron restaurados correctamente. Reinicia DentalCare para asegurar que todas las vistas carguen la información restaurada.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mostrarError("No fue posible restaurar el respaldo", ex.getMessage());
        }
    }

    @FXML
    private void abrirCarpetaRespaldo() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleccionar carpeta de respaldo");
        chooser.showDialog(null);
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje == null ? "Ocurrió un error inesperado." : mensaje);
        alert.showAndWait();
    }
}
