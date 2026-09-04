package mx.dentalcare.ui.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.util.Duration;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LoginController {

    @FXML private StackPane authRoot;
    @FXML private HBox authShell;
    @FXML private PasswordField txtPassword;
    @FXML private Label lblError;

    private double mouseOffsetX;
    private double mouseOffsetY;

    private final AuthenticationService authenticationService;
    private final ApplicationContext context;

    public LoginController(AuthenticationService authenticationService, ApplicationContext context) {
        this.authenticationService = authenticationService;
        this.context = context;
    }

    @FXML
    private void initialize() {
        String overrides = getClass().getResource("/ui/css/dentalcare-overrides.css").toExternalForm();
        if (!authRoot.getStylesheets().contains(overrides)) {
            authRoot.getStylesheets().add(overrides);
        }
    }

    @FXML
    private void iniciarMovimientoVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        mouseOffsetX = event.getScreenX() - stage.getX();
        mouseOffsetY = event.getScreenY() - stage.getY();
    }

    @FXML
    private void moverVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        stage.setX(event.getScreenX() - mouseOffsetX);
        stage.setY(event.getScreenY() - mouseOffsetY);
    }

    @FXML
    private void iniciarSesion() {
        ocultarError();
        String password = txtPassword.getText();

        try {
            authenticationService.login(password);
            mostrarTransicion();
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

    @FXML
    private void cerrarVentana() {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        stage.close();
    }

    private void mostrarTransicion() {
        txtPassword.setDisable(true);

        StackPane overlay = new StackPane();
        overlay.getStyleClass().add("auth-transition-overlay");
        overlay.setOpacity(0);

        StackPane contenido = new StackPane();
        contenido.getStyleClass().add("auth-transition-content");

        ProgressIndicator indicador = new ProgressIndicator();
        indicador.setProgress(-1);
        indicador.getStyleClass().add("auth-progress");

        Label titulo = new Label("DentalCare");
        titulo.getStyleClass().add("auth-transition-title");
        Label mensaje = new Label("Preparando tu espacio de trabajo...");
        mensaje.getStyleClass().add("auth-transition-message");

        javafx.scene.layout.VBox textos = new javafx.scene.layout.VBox(6, titulo, mensaje);
        textos.setAlignment(javafx.geometry.Pos.CENTER);
        javafx.scene.layout.VBox grupo = new javafx.scene.layout.VBox(18, indicador, textos);
        grupo.setAlignment(javafx.geometry.Pos.CENTER);

        contenido.getChildren().add(grupo);
        overlay.getChildren().add(contenido);
        authRoot.getChildren().add(overlay);

        FadeTransition entrada = new FadeTransition(Duration.millis(220), overlay);
        entrada.setFromValue(0);
        entrada.setToValue(1);
        entrada.play();

        PauseTransition pausa = new PauseTransition(Duration.millis(420));
        pausa.setOnFinished(event -> {
            try {
                abrirAplicacion();
            } catch (Exception e) {
                authRoot.getChildren().remove(overlay);
                txtPassword.setDisable(false);
                mostrarError("No fue posible abrir DentalCare. Verifica la configuración de seguridad.");
            }
        });
        pausa.play();
    }

    private void abrirAplicacion() throws Exception {
        Stage loginStage = (Stage) txtPassword.getScene().getWindow();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/MainView.fxml"));
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);

        Stage applicationStage = new Stage();
        applicationStage.setTitle("DentalCare");
        applicationStage.setScene(scene);
        applicationStage.setMinWidth(1000);
        applicationStage.setMinHeight(650);
        applicationStage.setResizable(true);
        applicationStage.show();
        applicationStage.centerOnScreen();

        loginStage.close();

        root.setOpacity(0);
        FadeTransition salida = new FadeTransition(Duration.millis(260), root);
        salida.setFromValue(0);
        salida.setToValue(1);
        salida.play();
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
