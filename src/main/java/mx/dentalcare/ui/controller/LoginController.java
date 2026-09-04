package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.stage.Stage;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @FXML
    private PasswordField txtPassword;

    @FXML
    private Label lblError;

    private final AuthenticationService authenticationService;
    private final ApplicationContext context;

    public LoginController(
            AuthenticationService authenticationService,
            ApplicationContext context
    ) {
        this.authenticationService = authenticationService;
        this.context = context;
    }

    @FXML
    private void iniciarSesion() {
        ocultarError();

        String password = txtPassword.getText();

        try {
            authenticationService.login(password);
            abrirAplicacion();
        } catch (SecurityException e) {
            mostrarError("La contraseña es incorrecta.");
            txtPassword.clear();
            txtPassword.requestFocus();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("No fue posible iniciar DentalCare. Verifica la configuración de seguridad.");
        }
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
