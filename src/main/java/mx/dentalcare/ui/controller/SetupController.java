package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SetupController {

    @FXML
    private VBox legacySection;

    @FXML
    private PasswordField txtLegacyPassword;

    @FXML
    private PasswordField txtPassword;

    @FXML
    private PasswordField txtConfirmPassword;

    @FXML
    private Label lblError;

    private final AuthenticationService authenticationService;
    private final ApplicationContext context;

    public SetupController(
            AuthenticationService authenticationService,
            ApplicationContext context
    ) {
        this.authenticationService = authenticationService;
        this.context = context;
    }

    @FXML
    public void initialize() {
        boolean migrationRequired = authenticationService.requiresLegacyMigration();
        legacySection.setManaged(migrationRequired);
        legacySection.setVisible(migrationRequired);
    }

    @FXML
    private void crearAdministrador() {
        ocultarError();

        String password = txtPassword.getText();
        String confirmPassword = txtConfirmPassword.getText();
        String legacyPassword = txtLegacyPassword.getText();

        if (password == null || password.length() < 8) {
            mostrarError("La nueva contraseña debe tener al menos 8 caracteres.");
            txtPassword.requestFocus();
            return;
        }

        if (!password.equals(confirmPassword)) {
            mostrarError("Las contraseñas no coinciden.");
            txtConfirmPassword.clear();
            txtConfirmPassword.requestFocus();
            return;
        }

        try {
            authenticationService.setupAdmin(password, legacyPassword);
            abrirAplicacion();
        } catch (RuntimeException e) {
            mostrarError(mensajeDeError(e));
            if (authenticationService.requiresLegacyMigration()) {
                txtLegacyPassword.requestFocus();
            }
        }
    }

    private String mensajeDeError(RuntimeException e) {
        String message = e.getMessage();
        if (message == null || message.isBlank()) {
            return "No fue posible configurar la seguridad de DentalCare.";
        }
        return message;
    }

    private void abrirAplicacion() throws Exception {
        Stage stage = (Stage) txtPassword.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/ui/fxml/MainView.fxml")
        );
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);

        stage.setTitle("DentalCare");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
        lblError.setManaged(true);
        lblError.setVisible(true);
    }

    private void ocultarError() {
        lblError.setText("");
        lblError.setManaged(false);
        lblError.setVisible(false);
    }
}
