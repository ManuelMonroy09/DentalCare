package mx.dentalcare.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.DialogPane;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.stage.Window;
import mx.dentalcare.DentalCareApplication;
import mx.dentalcare.security.SecuritySession;
import mx.dentalcare.security.AuthenticationService;
import mx.dentalcare.ui.util.WindowIconUtil;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class DentalCareJavaFXApplication extends Application {

    private ConfigurableApplicationContext context;
    private String hojaEstilosBase;
    private String hojaEstilosOverrides;
    private String hojaEstilosDialogos;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(DentalCareApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        WindowIconUtil.inicializar();
        instalarEstiloGlobalDeDialogos();

        AuthenticationService authenticationService = context.getBean(AuthenticationService.class);

        if (authenticationService.isConfigured()) {
            stage.initStyle(StageStyle.UNDECORATED);
            mostrarVista(stage, "/ui/fxml/LoginView.fxml", "DentalCare | Iniciar sesión", 900, 540);
        } else {
            stage.initStyle(StageStyle.UNDECORATED);
            mostrarVista(stage, "/ui/fxml/SetupView.fxml", "DentalCare | Configuración inicial", 900, 540);
        }
    }

    private void instalarEstiloGlobalDeDialogos() {
        hojaEstilosBase = getClass()
                .getResource("/ui/css/dentalcare.css")
                .toExternalForm();

        hojaEstilosOverrides = getClass()
                .getResource("/ui/css/dentalcare-overrides.css")
                .toExternalForm();

        hojaEstilosDialogos = getClass()
                .getResource("/ui/css/dialog.css")
                .toExternalForm();

        Window.getWindows().addListener((ListChangeListener<Window>) cambio -> {
            while (cambio.next()) {
                if (!cambio.wasAdded()) {
                    continue;
                }

                for (Window window : cambio.getAddedSubList()) {
                    Platform.runLater(() -> aplicarEstiloDentalCare(window));
                }
            }
        });
    }

    private void aplicarEstiloDentalCare(Window window) {
        if (window == null || window.getScene() == null) {
            return;
        }

        if (!window.getScene().getStylesheets().contains(hojaEstilosBase)) {
            window.getScene().getStylesheets().add(hojaEstilosBase);
        }

        if (!window.getScene().getStylesheets().contains(hojaEstilosOverrides)) {
            window.getScene().getStylesheets().add(hojaEstilosOverrides);
        }

        if (!window.getScene().getStylesheets().contains(hojaEstilosDialogos)) {
            window.getScene().getStylesheets().add(hojaEstilosDialogos);
        }

        if (window.getScene().getRoot() instanceof DialogPane pane) {
            if (!pane.getStyleClass().contains("standard-dialog")) {
                pane.getStyleClass().add("standard-dialog");
            }

            pane.setPrefWidth(520);
            pane.setMinWidth(520);

            Button ok = (Button) pane.lookupButton(ButtonType.OK);
            if (ok != null) {
                ok.getStyleClass().add("dialog-primary-button");
            }

            Button cancel = (Button) pane.lookupButton(ButtonType.CANCEL);
            if (cancel != null) {
                cancel.getStyleClass().add("dialog-secondary-button");
            }

            Button yes = (Button) pane.lookupButton(ButtonType.YES);
            if (yes != null) {
                yes.setText("Sí");
                yes.getStyleClass().add("dialog-primary-button");
            }

            Button no = (Button) pane.lookupButton(ButtonType.NO);
            if (no != null) {
                no.setText("No");
                no.getStyleClass().add("dialog-secondary-button");
            }

            pane.applyCss();
            pane.layout();
            window.sizeToScene();

            // El segundo ajuste permite que el alto calculado incluya
            // contenido envuelto y botones después de aplicar el ancho.
            Platform.runLater(() -> {
                if (window.getScene() != null) {
                    pane.applyCss();
                    pane.layout();
                    window.sizeToScene();
                }
            });
        }
    }

    private void mostrarVista(Stage stage, String ruta, String titulo, double ancho, double alto) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, ancho, alto);
        scene.setFill(Color.TRANSPARENT);

        if (!scene.getStylesheets().contains(hojaEstilosBase)) scene.getStylesheets().add(hojaEstilosBase);
        if (!scene.getStylesheets().contains(hojaEstilosOverrides)) scene.getStylesheets().add(hojaEstilosOverrides);
        if (!scene.getStylesheets().contains(hojaEstilosDialogos)) scene.getStylesheets().add(hojaEstilosDialogos);

        stage.setTitle(titulo);
        stage.setScene(scene);
        stage.setMinWidth(ancho);
        stage.setMinHeight(alto);
        stage.setMaxWidth(ancho);
        stage.setMaxHeight(alto);
        stage.setResizable(false);
        stage.show();
        stage.centerOnScreen();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.getBean(SecuritySession.class).clear();
            context.close();
        }
        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
