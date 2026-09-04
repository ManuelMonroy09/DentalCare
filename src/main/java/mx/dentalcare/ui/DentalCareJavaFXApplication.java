package mx.dentalcare.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import mx.dentalcare.DentalCareApplication;
import mx.dentalcare.security.SecuritySession;
import mx.dentalcare.security.AuthenticationService;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

public class DentalCareJavaFXApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(DentalCareApplication.class).run();
    }

    @Override
    public void start(Stage stage) throws Exception {
        AuthenticationService authenticationService = context.getBean(AuthenticationService.class);

        if (authenticationService.isConfigured()) {
            mostrarVista(stage, "/ui/fxml/LoginView.fxml", "DentalCare | Iniciar sesión", 960, 620);
        } else {
            mostrarVista(stage, "/ui/fxml/SetupView.fxml", "DentalCare | Configuración inicial", 1200, 750);
        }
    }

    private void mostrarVista(Stage stage, String ruta, String titulo, double ancho, double alto) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
        loader.setControllerFactory(context::getBean);

        Parent root = loader.load();
        Scene scene = new Scene(root, ancho, alto);

        stage.setTitle(titulo);
        stage.setScene(scene);

        if (ruta.endsWith("LoginView.fxml")) {
            stage.setMinWidth(960);
            stage.setMinHeight(620);
            stage.setResizable(false);
        } else {
            stage.setMinWidth(1000);
            stage.setMinHeight(650);
        }

        stage.show();
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
