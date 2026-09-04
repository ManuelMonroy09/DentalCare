package mx.dentalcare.ui;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.Parent;
import javafx.stage.Stage;
import mx.dentalcare.DentalCareApplication;
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

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/MainView.fxml"));

        loader.setControllerFactory(context::getBean);
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 750);
        stage.setTitle("DentalCare");
        stage.setScene(scene);
        stage.setMinWidth(1000);
        stage.setMinHeight(650);
        stage.show();
    }

    @Override
    public void stop() {

        context.close();

        Platform.exit();
    }

    public static void main(String[] args) {
        launch(args);
    }
}