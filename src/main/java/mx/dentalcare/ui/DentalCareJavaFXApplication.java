package mx.dentalcare.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.collections.ListChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
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
    private String hojaEstilosDentalCare;

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
        hojaEstilosDentalCare = getClass()
                .getResource("/ui/css/dentalcare-overrides.css")
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

        if (!window.getScene().getStylesheets().contains(hojaEstilosDentalCare)) {
            window.getScene().getStylesheets().add(hojaEstilosDentalCare);
        }
    }

    private void mostrarVista(Stage stage, String ruta, String titulo, double ancho, double alto) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, ancho, alto);
        scene.setFill(Color.TRANSPARENT);

        String overrides = getClass().getResource("/ui/css/dentalcare-overrides.css").toExternalForm();
        if (!scene.getStylesheets().contains(overrides)) scene.getStylesheets().add(overrides);

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
