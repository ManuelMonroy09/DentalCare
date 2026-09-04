package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.paciente.exception.PacienteValidationException;
import mx.dentalcare.service.PacientesService;
import org.springframework.context.annotation.Scope;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.Optional;

@Component
@Scope(ConfigurableBeanFactory.SCOPE_PROTOTYPE)
public class PacienteDialogController {

    @FXML
    private Label tituloLabel;

    @FXML
    private Label errorLabel;

    @FXML
    private TextField nombreField;

    @FXML
    private TextField apellidoPaternoField;

    @FXML
    private TextField apellidoMaternoField;

    @FXML
    private TextField telefonoField;

    @FXML
    private TextField emailField;

    @FXML
    private Button guardarButton;

    @FXML
    private Button cancelarButton;

    private Paciente paciente;

    private final PacientesService pacientesService;
    private String nombreOriginal = "";
    private String apellidoPaternoOriginal = "";
    private String apellidoMaternoOriginal = "";
    private String telefonoOriginal = "";
    private String emailOriginal = "";

    private boolean cierreAutorizado = false;

    public PacienteDialogController(PacientesService pacientesService) {
        this.pacientesService = pacientesService;
    }

    @FXML
    public void initialize() {

        errorLabel.setText("");
        guardarButton.setOnAction(event -> guardar());
        cancelarButton.setOnAction(event -> intentarCerrar());
        configurarValidacionCampos();
        actualizarEstadoGuardar();
        javafx.application.Platform.runLater(this::configurarCierreVentana);
    }

    private void configurarCierreVentana() {

        if (guardarButton.getScene() == null) {
            return;
        }

        Stage stage = (Stage) guardarButton.getScene().getWindow();
        stage.setOnCloseRequest(event -> {
            if (cierreAutorizado) {
                return;
            }
            event.consume();
            intentarCerrar();
        });
    }

    private void configurarValidacionCampos() {

        nombreField.textProperty().addListener((obs, oldValue, newValue) -> {limpiarErrorCampo(nombreField);actualizarEstadoGuardar();});
        apellidoPaternoField.textProperty().addListener((obs, oldValue, newValue) -> {limpiarErrorCampo(apellidoPaternoField);actualizarEstadoGuardar();});
        apellidoMaternoField.textProperty().addListener((obs, oldValue, newValue) -> {limpiarErrorCampo(apellidoMaternoField);actualizarEstadoGuardar();});
        telefonoField.textProperty().addListener((obs, oldValue, newValue) -> {actualizarEstadoGuardar();
                    if (newValue.isBlank()) {
                        limpiarErrorCampo(telefonoField);
                        if (errorLabel.getText().contains("teléfono")) {
                            errorLabel.setText("");
                        }
                        return;
                    }
                    if (!newValue.matches("\\d{10}")) {
                        marcarError(telefonoField);
                        errorLabel.setText("El teléfono debe contener exactamente 10 dígitos.");
                    } else {
                        limpiarErrorCampo(telefonoField);
                        if (errorLabel.getText().contains("teléfono")) {
                            errorLabel.setText("");
                        }
                    }
                }
        );

        emailField.textProperty().addListener((obs, oldValue, newValue) -> {limpiarErrorCampo(emailField);actualizarEstadoGuardar();});
    }

    private void limpiarErrorCampo(TextField campo) {
        campo.getStyleClass().remove("field-error");
    }

    private void actualizarEstadoGuardar() {

        boolean camposValidos =
                !nombreField.getText().trim().isEmpty()
                        && !apellidoPaternoField.getText().trim().isEmpty()
                        && !apellidoMaternoField.getText().trim().isEmpty()
                        && telefonoField.getText().trim().matches("\\d{10}");

        guardarButton.setDisable(!camposValidos);
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
        tituloLabel.setText("Editar Paciente");
        nombreField.setText(valorSeguro(paciente.getNombre()));
        apellidoPaternoField.setText(valorSeguro(paciente.getApellidoPaterno()));
        apellidoMaternoField.setText(valorSeguro(paciente.getApellidoMaterno()));
        telefonoField.setText(valorSeguro(paciente.getTelefono()));
        emailField.setText(valorSeguro(paciente.getEmail()));
        guardarValoresOriginales();
        actualizarEstadoGuardar();
    }

    private void guardarValoresOriginales() {
        nombreOriginal = obtenerValorCampo(nombreField);
        apellidoPaternoOriginal = obtenerValorCampo(apellidoPaternoField);
        apellidoMaternoOriginal = obtenerValorCampo(apellidoMaternoField);
        telefonoOriginal = obtenerValorCampo(telefonoField);
        emailOriginal = obtenerValorCampo(emailField);
    }

    private String obtenerValorCampo(TextField campo) {
        return campo.getText() == null ? "" : campo.getText();
    }

    private String valorSeguro(String valor) {
        return valor == null ? "" : valor;
    }

    private boolean hayCambiosSinGuardar() {

        return !obtenerValorCampo(nombreField).equals(nombreOriginal)
                || !obtenerValorCampo(apellidoPaternoField)
                .equals(apellidoPaternoOriginal)
                || !obtenerValorCampo(apellidoMaternoField)
                .equals(apellidoMaternoOriginal)
                || !obtenerValorCampo(telefonoField)
                .equals(telefonoOriginal)
                || !obtenerValorCampo(emailField)
                .equals(emailOriginal);
    }

    private void intentarCerrar() {
        if (!hayCambiosSinGuardar()) {
            cerrarVentana();
            return;
        }
        Alert confirmacion = new Alert(Alert.AlertType.CONFIRMATION);
        confirmacion.setTitle("Cambios sin guardar");
        confirmacion.setHeaderText("Hay cambios sin guardar.");
        confirmacion.setContentText("¿Qué deseas hacer con los cambios realizados?");
        ButtonType guardarButton = new ButtonType("Guardar");
        ButtonType noGuardarButton = new ButtonType("No guardar");
        ButtonType cancelarButton = new ButtonType("Cancelar");
        confirmacion.getButtonTypes().setAll(guardarButton, noGuardarButton, cancelarButton);
        Optional<ButtonType> resultado = confirmacion.showAndWait();
        if (resultado.isEmpty()) {
            return;
        }
        if (resultado.get() == guardarButton) {
            guardar();
        } else if (resultado.get() == noGuardarButton) {
            cerrarVentana();
        }
    }

    private void guardar() {
        limpiarError();
        try {
            Long id = paciente != null ? paciente.getId() : null;
            Paciente pacienteFormulario = new Paciente(
                    id,
                    normalizarNombre(nombreField.getText()),
                    normalizarNombre(apellidoPaternoField.getText()),
                    normalizarNombre(apellidoMaternoField.getText()),
                    telefonoField.getText().trim(),
                    emailField.getText().trim()
            );
            pacientesService.guardar(pacienteFormulario);
            paciente = pacienteFormulario;
            guardarValoresOriginales();
            cerrarVentana();
        } catch (PacienteValidationException e) {
            mostrarError(e.getMessage());
            marcarCampoConError(e.getMessage());
        } catch (Exception e) {
            mostrarError("No fue posible guardar el paciente.");
            e.printStackTrace();
        }
    }

    private void mostrarError(String mensaje) {
        errorLabel.setText(mensaje);
    }

    private void limpiarError() {
        errorLabel.setText("");
        nombreField.getStyleClass().remove("field-error");
        apellidoPaternoField.getStyleClass().remove("field-error");
        apellidoMaternoField.getStyleClass().remove("field-error");
        telefonoField.getStyleClass().remove("field-error");
        emailField.getStyleClass().remove("field-error");
    }

    private void cerrarVentana() {
        Stage stage = (Stage) guardarButton.getScene().getWindow();
        cierreAutorizado = true;
        stage.close();
    }

    public Paciente getPaciente() {
        return paciente;
    }

    private void marcarCampoConError(String mensaje) {
        if (mensaje.contains("nombre")) {
            marcarError(nombreField);
        } else if (mensaje.contains("apellido paterno")) {
            marcarError(apellidoPaternoField);
        } else if (mensaje.contains("apellido materno")) {
            marcarError(apellidoMaternoField);
        } else if (mensaje.contains("teléfono")) {
            marcarError(telefonoField);
        } else if (mensaje.contains("correo")) {
            marcarError(emailField);
        }
    }

    private void marcarError(TextField campo) {
        if (!campo.getStyleClass().contains("field-error")) {
            campo.getStyleClass().add("field-error");
        }
    }

    private String normalizarNombre(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        String[] palabras = texto.trim().toLowerCase(Locale.ROOT).split("\\s+");
        StringBuilder resultado = new StringBuilder();
        for (String palabra : palabras) {
            if (resultado.length() > 0) {
                resultado.append(" ");
            }
            resultado.append(capitalizarPalabra(palabra));
        }
        return resultado.toString();
    }

    private String capitalizarPalabra(String palabra) {
        StringBuilder resultado = new StringBuilder();
        boolean capitalizar = true;
        for (char caracter : palabra.toCharArray()) {
            if (caracter == '-' || caracter == '\'') {
                resultado.append(caracter);
                capitalizar = true;
                continue;
            }
            if (capitalizar) {
                resultado.append(Character.toUpperCase(caracter));
                capitalizar = false;
            } else {
                resultado.append(caracter);
            }
        }
        return resultado.toString();
    }
}