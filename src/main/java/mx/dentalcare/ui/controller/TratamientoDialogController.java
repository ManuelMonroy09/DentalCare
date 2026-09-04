package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.service.TratamientoService;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;
import java.math.BigDecimal;

@Component
@Scope("prototype")
public class TratamientoDialogController {

    @FXML private Label tituloLabel;
    @FXML private TextField nombreField;
    @FXML private TextArea descripcionArea;
    @FXML private TextField precioField;
    @FXML private Spinner<Integer> duracionSpinner;
    @FXML private Label lblError;
    @FXML private Button cancelarButton;
    @FXML private Button guardarButton;

    private final TratamientoService tratamientoService;
    private Tratamiento tratamiento;

    public TratamientoDialogController(TratamientoService tratamientoService) {
        this.tratamientoService = tratamientoService;
    }

    @FXML
    public void initialize() {
        configurarDuracion();
        guardarButton.setOnAction(event -> guardar());
        cancelarButton.setOnAction(event -> cerrar());
    }

    private void configurarDuracion() {
        SpinnerValueFactory.IntegerSpinnerValueFactory factory =
                new SpinnerValueFactory.IntegerSpinnerValueFactory(5, 480, 60, 5);
        duracionSpinner.setValueFactory(factory);
    }

    public void setTratamiento(Tratamiento tratamiento) {
        this.tratamiento = tratamiento;
        tituloLabel.setText("Editar tratamiento");
        nombreField.setText(tratamiento.getNombre());
        descripcionArea.setText(tratamiento.getDescripcion() != null ? tratamiento.getDescripcion() : "");
        precioField.setText(tratamiento.getPrecio() != null ? tratamiento.getPrecio().toPlainString() : "");
        duracionSpinner.getValueFactory().setValue(tratamiento.getDuracionMinutos());
    }

    private void guardar() {
        try {
            String nombre = nombreField.getText();
            String descripcion = descripcionArea.getText();
            String precioTexto = precioField.getText();

            if (nombre == null || nombre.isBlank()) {
                mostrarError("El nombre del tratamiento es obligatorio.");
                return;
            }
            if (precioTexto == null || precioTexto.isBlank()) {
                mostrarError("El precio del tratamiento es obligatorio.");
                return;
            }

            BigDecimal precio;
            try {
                precio = new BigDecimal(precioTexto.trim().replace(",", "."));
            } catch (NumberFormatException e) {
                mostrarError("El precio debe ser un número válido.");
                return;
            }

            if (precio.compareTo(BigDecimal.ZERO) < 0) {
                mostrarError("El precio no puede ser negativo.");
                return;
            }

            Integer duracion = duracionSpinner.getValue();
            if (duracion == null || duracion <= 0) {
                mostrarError("La duración debe ser mayor a 0 minutos.");
                return;
            }

            if (tratamiento == null) {
                Tratamiento nuevo = new Tratamiento();
                nuevo.setNombre(nombre.trim());
                nuevo.setDescripcion(descripcion);
                nuevo.setPrecio(precio);
                nuevo.setDuracionMinutos(duracion);
                tratamientoService.crear(nuevo);
            } else {
                tratamiento.setNombre(nombre.trim());
                tratamiento.setDescripcion(descripcion);
                tratamiento.setPrecio(precio);
                tratamiento.setDuracionMinutos(duracion);
                tratamientoService.actualizar(tratamiento);
            }

            cerrar();
        } catch (Exception e) {
            mostrarError(e.getMessage() != null ? e.getMessage() : "No fue posible guardar el tratamiento.");
        }
    }

    private void mostrarError(String mensaje) {
        lblError.setText(mensaje);
    }

    private void cerrar() {
        Stage stage = (Stage) guardarButton.getScene().getWindow();
        stage.close();
    }
}
