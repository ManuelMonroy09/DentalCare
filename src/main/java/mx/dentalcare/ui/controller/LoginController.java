package mx.dentalcare.ui.controller;

import javafx.animation.FadeTransition;
import javafx.animation.PauseTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class LoginController {
    @FXML private StackPane authRoot;
    @FXML private TextField txtUsername;
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

    @FXML private void initialize() {
        String overrides = getClass().getResource("/ui/css/dentalcare-overrides.css").toExternalForm();
        if (!authRoot.getStylesheets().contains(overrides)) authRoot.getStylesheets().add(overrides);
    }

    @FXML private void iniciarMovimientoVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        mouseOffsetX = event.getScreenX() - stage.getX();
        mouseOffsetY = event.getScreenY() - stage.getY();
    }

    @FXML private void moverVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        stage.setX(event.getScreenX() - mouseOffsetX);
        stage.setY(event.getScreenY() - mouseOffsetY);
    }

    @FXML private void iniciarSesion() {
        ocultarError();
        String username = txtUsername.getText();
        String password = txtPassword.getText();
        try {
            AuthenticatedUser user = authenticationService.login(username, password);
            if (user.isAdmin() && !authenticationService.hasRecoveryKey()) {
                mostrarClaveRecuperacion(authenticationService.generateRecoveryKey(), true);
            }
            mostrarTransicion();
        } catch (SecurityException e) {
            mostrarError("El usuario o la contraseña son incorrectos.");
            txtPassword.clear();
            txtPassword.requestFocus();
        } catch (IllegalArgumentException e) {
            mostrarError(e.getMessage());
        } catch (Exception e) {
            mostrarError("No fue posible iniciar DentalCare. Verifica la configuración de seguridad.");
        }
    }

    @FXML private void olvidoContrasena() {
        mostrarDialogoRecuperacion();
    }

    @FXML private void cerrarVentana() {
        Stage stage = (Stage) authRoot.getScene().getWindow();
        stage.close();
    }

    private void mostrarDialogoRecuperacion() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Recuperar acceso");
        dialog.setHeaderText("Restablecer contraseña del administrador");
        dialog.getDialogPane().getStyleClass().add("standard-dialog");
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);

        ButtonType resetButton = new ButtonType("Restablecer contraseña", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(resetButton, ButtonType.CANCEL);
        Node resetNode = dialog.getDialogPane().lookupButton(resetButton);
        Node cancelNode = dialog.getDialogPane().lookupButton(ButtonType.CANCEL);
        resetNode.getStyleClass().add("dialog-primary-button");
        cancelNode.getStyleClass().add("dialog-secondary-button");

        Label ayuda = new Label("Introduce la clave de recuperación que se generó al configurar DentalCare.");
        ayuda.setWrapText(true);
        TextField recoveryField = new TextField();
        recoveryField.setPromptText("Clave de recuperación");
        recoveryField.setMaxWidth(Double.MAX_VALUE);
        PasswordField newPassword = new PasswordField();
        newPassword.setPromptText("Mínimo 8 caracteres");
        PasswordField confirmPassword = new PasswordField();
        confirmPassword.setPromptText("Repite la nueva contraseña");
        Label error = new Label();
        error.getStyleClass().add("standard-error");
        error.setWrapText(true);
        error.setMinHeight(34);
        error.setPrefHeight(34);
        error.setMaxHeight(50);
        error.setManaged(true);
        error.setVisible(false);

        VBox content = new VBox(10,
                ayuda,
                new Label("Clave de recuperación"), recoveryField,
                new Label("Nueva contraseña"), newPassword,
                new Label("Confirmar contraseña"), confirmPassword,
                error);
        content.setPrefWidth(440);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefHeight(440);
        dialog.getDialogPane().setMinHeight(440);

        resetNode.addEventFilter(ActionEvent.ACTION, event -> {
            String recoveryKey = recoveryField.getText();
            String password = newPassword.getText();
            String confirmation = confirmPassword.getText();

            if (recoveryKey == null || recoveryKey.isBlank()) {
                mostrarErrorDialog(error, "La clave de recuperación es obligatoria.");
                event.consume();
                return;
            }
            if (password == null || password.length() < 8) {
                mostrarErrorDialog(error, "La nueva contraseña debe tener al menos 8 caracteres.");
                event.consume();
                return;
            }
            if (!password.equals(confirmation)) {
                mostrarErrorDialog(error, "Las contraseñas no coinciden.");
                event.consume();
                return;
            }

            try {
                String newRecoveryKey = authenticationService.recoverAdminPassword(recoveryKey, password);
                dialog.close();
                mostrarClaveRecuperacion(newRecoveryKey, false);
                mostrarTransicion();
            } catch (SecurityException e) {
                mostrarErrorDialog(error, "La clave de recuperación es incorrecta o no es válida.");
                event.consume();
            } catch (IllegalArgumentException e) {
                mostrarErrorDialog(error, e.getMessage());
                event.consume();
            } catch (Exception e) {
                mostrarErrorDialog(error, "No fue posible restablecer la contraseña. Los datos no fueron modificados.");
                event.consume();
            }
        });

        dialog.setOnShown(event -> recoveryField.requestFocus());
        dialog.showAndWait();
    }

    private void mostrarErrorDialog(Label error, String message) {
        error.setText(message == null || message.isBlank() ? "No fue posible completar la operación." : message);
        error.setManaged(true);
        error.setVisible(true);
    }

    private void mostrarClaveRecuperacion(String recoveryKey, boolean initialSetup) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Clave de recuperación");
        dialog.setHeaderText("Guarda esta clave en un lugar seguro");
        dialog.getDialogPane().getStyleClass().add("standard-dialog");
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        Node okNode = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okNode.getStyleClass().add("dialog-primary-button");

        Label message = new Label(initialSetup
                ? "Esta clave permite recuperar el acceso del administrador si se olvida la contraseña. No la guardes dentro de DentalCare ni la compartas."
                : "La contraseña fue restablecida. Por seguridad se generó una nueva clave de recuperación. Guarda la nueva clave y sustituye la anterior.");
        message.setWrapText(true);
        message.setMaxWidth(430);

        TextField keyField = new TextField(recoveryKey);
        keyField.setEditable(false);
        keyField.setMaxWidth(Double.MAX_VALUE);
        keyField.setOnMouseClicked(event -> keyField.selectAll());

        Button copyButton = new Button("Copiar clave");
        copyButton.getStyleClass().add("dialog-secondary-button");
        copyButton.setOnAction(event -> {
            Clipboard.getSystemClipboard().setContent(java.util.Map.of(DataFormat.PLAIN_TEXT, recoveryKey));
            keyField.selectAll();
        });

        VBox content = new VBox(12, message, keyField, copyButton);
        content.setPrefWidth(430);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefHeight(330);
        dialog.getDialogPane().setMinHeight(330);
        dialog.showAndWait();
    }

    private void mostrarTransicion() {
        txtUsername.setDisable(true);
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
        VBox textos = new VBox(6, titulo, mensaje);
        textos.setAlignment(javafx.geometry.Pos.CENTER);
        VBox grupo = new VBox(18, indicador, textos);
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
                txtUsername.setDisable(false);
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
        Scene scene = new Scene(root, 1280, 800);
        Stage applicationStage = new Stage();
        applicationStage.setTitle("DentalCare");
        applicationStage.setScene(scene);
        applicationStage.setMinWidth(1100);
        applicationStage.setMinHeight(700);
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
