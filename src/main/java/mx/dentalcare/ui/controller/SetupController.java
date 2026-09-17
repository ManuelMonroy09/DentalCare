package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
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
        if (!mostrarAcuerdoLicencia()) {
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

    private boolean mostrarAcuerdoLicencia() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Acuerdo de licencia de uso");
        dialog.setHeaderText("DentalCare | Acuerdo de licencia de uso");
        dialog.getDialogPane().getStyleClass().add("standard-dialog");
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        dialog.getDialogPane().getStylesheets().add(dialogCss);

        ButtonType aceptar = new ButtonType("Aceptar y continuar", ButtonBar.ButtonData.OK_DONE);
        ButtonType rechazar = new ButtonType("No aceptar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialog.getDialogPane().getButtonTypes().addAll(aceptar, rechazar);

        TextArea acuerdo = new TextArea(ACUERDO_LICENCIA);
        acuerdo.setWrapText(true);
        acuerdo.setEditable(false);
        acuerdo.setFocusTraversable(false);
        acuerdo.setPrefRowCount(24);
        acuerdo.setPrefWidth(620);
        acuerdo.setPrefHeight(500);
        acuerdo.setMaxWidth(Double.MAX_VALUE);
        acuerdo.getStyleClass().add("recovery-info-text");

        CheckBox aceptacion = new CheckBox("He leído y acepto los términos de esta licencia de uso.");
        aceptacion.setWrapText(true);

        VBox content = new VBox(14, acuerdo, aceptacion);
        content.setPrefWidth(620);
        content.setMinHeight(540);
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().setPrefWidth(680);
        dialog.getDialogPane().setPrefHeight(650);
        dialog.getDialogPane().setMinHeight(650);

        Node aceptarNode = dialog.getDialogPane().lookupButton(aceptar);
        Node rechazarNode = dialog.getDialogPane().lookupButton(rechazar);
        aceptarNode.getStyleClass().addAll("dialog-primary-button", "dialog-license-accept-button");
        rechazarNode.getStyleClass().add("dialog-secondary-button");
        aceptarNode.setDisable(true);
        if (aceptarNode instanceof Button aceptarButton) {
            aceptarButton.setPrefWidth(210);
            aceptarButton.setMinWidth(210);
            aceptarButton.setMaxWidth(210);
        }
        aceptacion.selectedProperty().addListener((obs, oldValue, selected) -> aceptarNode.setDisable(!selected));

        final boolean[] resultado = {false};
        dialog.setResultConverter(button -> {
            if (button == aceptar && aceptacion.isSelected()) {
                resultado[0] = true;
                return aceptar;
            }
            return null;
        });

        dialog.showAndWait();
        return resultado[0];
    }

    private static final String ACUERDO_LICENCIA = """
            ACUERDO DE LICENCIA DE USO DE SOFTWARE

            DentalCare
            Versión 1.0.0

            © 2026 Juan Manuel Mendoza Monroy & Scarleth. Todos los derechos reservados.

            1. OBJETO Y TITULARIDAD

            Este Acuerdo regula el uso del programa de computación denominado DentalCare, incluyendo su código, interfaz, documentación, elementos gráficos y demás elementos originales que formen parte del Software.

            DentalCare es software de carácter propietario. Salvo componentes de terceros sujetos a sus propias licencias, los elementos originales del Software pertenecen a sus respectivos autores y titulares de derechos.

            Juan Manuel Mendoza Monroy & Scarleth se identifican como autores del desarrollo de DentalCare. Este Acuerdo no constituye una transmisión de derechos de autor ni una cesión de derechos patrimoniales sobre el Software.

            2. CONCESIÓN DE LICENCIA

            Al aceptar este Acuerdo se concede al usuario una licencia de uso no exclusiva y no transferible para utilizar DentalCare conforme a los términos establecidos en este documento.

            La licencia permite utilizar el Software para la gestión administrativa y operativa del consultorio dental del usuario, de acuerdo con las funciones disponibles en la versión instalada.

            3. USOS AUTORIZADOS

            El usuario podrá instalar y utilizar DentalCare en los equipos que legítimamente controle, utilizar sus funciones y realizar las copias de respaldo que resulten necesarias, siempre dentro de los límites permitidos por la legislación aplicable y esta licencia.

            4. RESTRICCIONES

            Salvo autorización expresa de los titulares o cuando la legislación aplicable permita lo contrario, el usuario no podrá:

            a) Comercializar, sublicenciar, alquilar o distribuir DentalCare o copias del Software.
            b) Redistribuir el Software atribuyéndose su autoría.
            c) Eliminar o alterar avisos de derechos de autor, titularidad o propiedad intelectual.
            d) Copiar, modificar o crear obras derivadas para distribuirlas o comercializarlas.
            e) Realizar ingeniería inversa, descompilación o desensamblado, salvo en los casos permitidos por la legislación aplicable.
            f) Eludir controles de seguridad, autenticación o restricciones de uso implementadas por el Software.

            5. DATOS DEL USUARIO

            El usuario conserva los derechos que legalmente le correspondan sobre la información y los datos que introduzca en DentalCare. La licencia no implica transferencia de propiedad sobre dichos datos.

            El usuario es responsable de la información que registre y de cumplir las obligaciones legales que le correspondan respecto de dicha información, incluyendo las relativas a los datos de sus pacientes.

            6. RESPONSABILIDAD SOBRE INFORMACIÓN CLÍNICA

            DentalCare es una herramienta informática de gestión y organización. El Software no sustituye el criterio profesional del odontólogo ni constituye por sí mismo un sistema de diagnóstico, tratamiento o decisión clínica.

            El usuario será responsable de verificar la información registrada y de utilizarla de conformidad con sus obligaciones profesionales y legales.

            7. SEGURIDAD Y CREDENCIALES

            El usuario es responsable de mantener bajo su control sus credenciales de acceso, contraseñas y claves de recuperación. No deberá compartirlas con personas no autorizadas.

            Cuando DentalCare permita administrar diferentes cuentas o permisos, el administrador será responsable de asignarlos de acuerdo con las necesidades de su organización.

            8. COMPONENTES DE TERCEROS

            DentalCare puede utilizar bibliotecas, frameworks, herramientas u otros componentes desarrollados por terceros. Dichos componentes pueden estar sujetos a sus propias licencias, que conservarán su validez y alcance.

            Nada de este Acuerdo pretende modificar o limitar los derechos concedidos por licencias de terceros que resulten aplicables.

            9. PROPIEDAD INTELECTUAL

            El nombre DentalCare, su identidad visual, interfaz, estructura, código, documentación y demás elementos originales del Software están protegidos por las disposiciones aplicables en materia de propiedad intelectual.

            La instalación o utilización del Software no otorga al usuario derechos de propiedad sobre DentalCare ni sobre sus elementos originales.

            10. DISPONIBILIDAD Y GARANTÍAS

            El Software se proporciona de acuerdo con las características y funcionalidades disponibles en la versión instalada. Las características pueden modificarse, mejorarse o corregirse en versiones posteriores.

            Salvo que se establezca expresamente lo contrario por escrito, esta licencia no constituye una garantía de disponibilidad permanente, ausencia absoluta de errores o compatibilidad futura con cualquier sistema operativo, hardware o software de terceros, sin perjuicio de los derechos que la legislación aplicable otorgue al usuario.

            11. TERMINACIÓN

            La licencia podrá terminar cuando el usuario incumpla sustancialmente las condiciones establecidas en este Acuerdo o cuando la legislación aplicable así lo determine.

            La terminación no afectará los derechos que legalmente correspondan al usuario sobre sus propios datos.

            12. LEGISLACIÓN APLICABLE

            Este Acuerdo se interpretará de conformidad con las leyes aplicables de los Estados Unidos Mexicanos, sin perjuicio de los derechos irrenunciables que correspondan al usuario conforme a disposiciones imperativas aplicables.

            13. ACEPTACIÓN

            Al seleccionar «Acepto y continuar», el usuario manifiesta que ha tenido acceso a este Acuerdo, que lo ha leído y que acepta utilizar DentalCare conforme a sus términos.

            El usuario reconoce que DentalCare es software propietario y que esta licencia no implica la transferencia de los derechos de propiedad intelectual sobre el Software.

            Si el usuario no acepta este Acuerdo, no podrá continuar con la configuración inicial de DentalCare.

            © 2026 Juan Manuel Mendoza Monroy & Scarleth
            Todos los derechos reservados.
            """;

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
