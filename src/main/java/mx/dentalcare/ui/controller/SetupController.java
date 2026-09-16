package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.input.Clipboard;
import javafx.scene.input.DataFormat;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SetupController {
    @FXML private HBox authShell;
    @FXML private VBox legacySection;
    @FXML private PasswordField txtLegacyPassword, txtPassword, txtConfirmPassword;
    @FXML private Label lblError;
    private double mouseOffsetX, mouseOffsetY;
    private final AuthenticationService authenticationService;
    private final ApplicationContext context;

    public SetupController(AuthenticationService authenticationService, ApplicationContext context) {
        this.authenticationService = authenticationService;
        this.context = context;
    }

    @FXML public void initialize() {
        boolean migrationRequired = authenticationService.requiresLegacyMigration();
        legacySection.setManaged(migrationRequired);
        legacySection.setVisible(migrationRequired);
    }

    @FXML private void iniciarMovimientoVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtPassword.getScene().getWindow();
        mouseOffsetX = event.getScreenX() - stage.getX();
        mouseOffsetY = event.getScreenY() - stage.getY();
    }

    @FXML private void moverVentana(javafx.scene.input.MouseEvent event) {
        Stage stage = (Stage) txtPassword.getScene().getWindow();
        stage.setX(event.getScreenX() - mouseOffsetX);
        stage.setY(event.getScreenY() - mouseOffsetY);
    }

    @FXML private void crearAdministrador() {
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
            String recoveryKey = authenticationService.setupAdmin(password, legacyPassword);
            mostrarClaveRecuperacion(recoveryKey);
            abrirAplicacion();
        } catch (RuntimeException e) {
            mostrarError(mensajeDeError(e));
            if (authenticationService.requiresLegacyMigration()) txtLegacyPassword.requestFocus();
        } catch (Exception e) {
            mostrarError("La configuración se completó, pero no fue posible abrir DentalCare.");
        }
    }

    @FXML private void cerrarVentana() {
        Stage stage = (Stage) txtPassword.getScene().getWindow();
        stage.close();
    }

    private String mensajeDeError(RuntimeException e) {
        String message = e.getMessage();
        return message == null || message.isBlank()
                ? "No fue posible configurar la seguridad de DentalCare."
                : message;
    }

    private void mostrarClaveRecuperacion(String recoveryKey) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Clave de recuperación");
        dialog.setHeaderText("Guarda esta clave antes de continuar");
        dialog.getDialogPane().getStyleClass().add("standard-dialog");
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);
        Node okNode = dialog.getDialogPane().lookupButton(ButtonType.OK);
        okNode.getStyleClass().add("dialog-primary-button");

        TextArea message = new TextArea(
                "Esta es la clave de recuperación del administrador. Permite recuperar el acceso si se olvida la contraseña. "
                        + "Guárdala fuera de DentalCare, por ejemplo en un lugar físico seguro o en un gestor de contraseñas. "
                        + "No la guardes dentro de DentalCare ni la compartas con otras personas. "
                        + "Si pierdes esta clave y también la contraseña, este mecanismo no podrá recuperar el acceso."
        );
        message.setWrapText(true);
        message.setEditable(false);
        message.setFocusTraversable(false);
        message.setPrefRowCount(5);
        message.setMinHeight(120);
        message.setPrefHeight(120);
        message.setMaxHeight(120);
        message.setMaxWidth(Double.MAX_VALUE);
        message.getStyleClass().add("recovery-info-text");

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

        VBox infoPanel = new VBox(message);
        infoPanel.getStyleClass().add("recovery-info-panel");
        infoPanel.setFillWidth(true);

        VBox content = new VBox(14, infoPanel, keyField, copyButton);
        content.setPrefWidth(450);
        content.setMinHeight(275);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefHeight(410);
        dialog.getDialogPane().setMinHeight(410);
        dialog.showAndWait();
    }

    private void abrirAplicacion() throws Exception {
        Stage setupStage = (Stage) txtPassword.getScene().getWindow();
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
        setupStage.close();
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
