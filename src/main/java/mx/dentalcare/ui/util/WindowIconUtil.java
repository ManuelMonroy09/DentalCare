package mx.dentalcare.ui.util;

import javafx.collections.ListChangeListener;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.Window;
import javafx.stage.WindowEvent;

import java.io.InputStream;

/**
 * Configura el icono de DentalCare en todas las ventanas JavaFX,
 * incluyendo ventanas secundarias, diálogos y alertas.
 */
public final class WindowIconUtil {

    private static final String ICON_RESOURCE = "/images/dentalcare-icon.png";
    private static final Image ICON = cargarIcono();
    private static boolean inicializado;

    private WindowIconUtil() {
    }

    public static void inicializar() {
        if (inicializado) {
            return;
        }

        inicializado = true;

        Window.getWindows().forEach(WindowIconUtil::configurarVentana);
        Window.getWindows().addListener(
                (ListChangeListener<Window>) change -> {
                    while (change.next()) {
                        if (change.wasAdded()) {
                            change.getAddedSubList()
                                    .forEach(WindowIconUtil::configurarVentana);
                        }
                    }
                }
        );
    }

    private static void configurarVentana(Window window) {
        if (!(window instanceof Stage stage)) {
            return;
        }

        stage.addEventHandler(
                WindowEvent.WINDOW_SHOWN,
                event -> aplicarIcono(stage)
        );

        aplicarIcono(stage);
    }

    private static void aplicarIcono(Stage stage) {
        stage.getIcons().setAll(ICON);
    }

    private static Image cargarIcono() {
        try (InputStream input = WindowIconUtil.class
                .getResourceAsStream(ICON_RESOURCE)) {

            if (input == null) {
                throw new IllegalStateException(
                        "No se encontró el recurso del icono de DentalCare: "
                                + ICON_RESOURCE
                );
            }

            return new Image(input);

        } catch (Exception e) {
            throw new IllegalStateException(
                    "No fue posible cargar el icono de DentalCare.",
                    e
            );
        }
    }
}
