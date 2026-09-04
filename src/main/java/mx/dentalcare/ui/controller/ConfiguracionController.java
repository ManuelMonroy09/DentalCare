package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import mx.dentalcare.service.BackupService;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Optional;

@Component
public class ConfiguracionController {

    private final BackupService backupService;

    public ConfiguracionController(BackupService backupService) {
        this.backupService = backupService;
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
