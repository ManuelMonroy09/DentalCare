package mx.dentalcare.ui.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Modality;
import javafx.stage.Stage;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class DetalleCitaController {

    private static final double ANCHO_VENTANA = 620;
    private static final double ALTO_VENTANA = 720;

    @FXML private Label lblPaciente;
    @FXML private Label lblFecha;
    @FXML private Label lblHorario;
    @FXML private Label lblDuracion;
    @FXML private Label lblMotivo;
    @FXML private Label lblTratamientos;
    @FXML private Label lblTotalTratamientos;
    @FXML private Label lblNotas;
    @FXML private Label lblEstado;
    @FXML private Button btnEditar;
    @FXML private Button btnCerrar;

    private final ApplicationContext applicationContext;
    private Cita cita;
    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy");
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public DetalleCitaController(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        btnCerrar.setOnAction(event -> cerrarVentana());
        btnEditar.setOnAction(event -> editarCita());
    }

    public void setCita(Cita cita) {
        this.cita = cita;
        mostrarCita();
        ajustarTamanoVentana();
    }

    private void ajustarTamanoVentana() {
        Platform.runLater(() -> {
            if (btnCerrar.getScene() == null || btnCerrar.getScene().getWindow() == null) {
                return;
            }

            Stage stage = (Stage) btnCerrar.getScene().getWindow();
            stage.setMinWidth(ANCHO_VENTANA);
            stage.setMinHeight(ALTO_VENTANA);
            stage.setMaxWidth(ANCHO_VENTANA);
            stage.setMaxHeight(ALTO_VENTANA);
            stage.setWidth(ANCHO_VENTANA);
            stage.setHeight(ALTO_VENTANA);
            stage.setResizable(false);
        });
    }

    private void mostrarCita() {
        if (cita == null) {
            return;
        }

        lblPaciente.setText(cita.getNombrePaciente());

        if (cita.getInicio() != null && cita.getFin() != null) {
            lblFecha.setText(capitalizar(cita.getInicio().format(FORMATO_FECHA)));
            lblHorario.setText(cita.getInicio().format(FORMATO_HORA) + " - " + cita.getFin().format(FORMATO_HORA));
        }

        lblDuracion.setText(cita.getDuracionMinutos() + " minutos");
        lblMotivo.setText(cita.getMotivo() != null && !cita.getMotivo().isBlank()
                ? cita.getMotivo()
                : "Sin motivo de consulta especificado");

        List<TratamientoAplicado> tratamientos = cita.getTratamientos();
        if (tratamientos == null || tratamientos.isEmpty()) {
            lblTratamientos.setText("Sin tratamientos registrados");
            lblTotalTratamientos.setText("$0.00");
        } else {
            String resumen = tratamientos.stream()
                    .filter(t -> t != null)
                    .map(TratamientoAplicado::getNombre)
                    .filter(nombre -> nombre != null && !nombre.isBlank())
                    .collect(Collectors.joining(", "));

            lblTratamientos.setText(resumen.isBlank() ? "Sin tratamientos registrados" : resumen);

            BigDecimal total = cita.obtenerTotalTratamientos();
            lblTotalTratamientos.setText("$" + total.setScale(2).toPlainString());
        }

        lblNotas.setText(cita.getNotas() != null && !cita.getNotas().isBlank()
                ? cita.getNotas()
                : "Sin notas");
        lblEstado.setText(formatearEstado(cita));
    }

    private void editarCita() {
        if (cita == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/NuevaCitaDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            NuevaCitaController controller = loader.getController();
            controller.prepararParaEdicion(cita);

            Stage stage = new Stage();
            stage.setTitle("Editar cita");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root, 720, 820));
            stage.setMinWidth(720);
            stage.setMinHeight(820);
            stage.setMaxWidth(720);
            stage.setMaxHeight(820);
            stage.setResizable(false);
            stage.showAndWait();
            mostrarCita();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatearEstado(Cita cita) {
        if (cita.getEstado() == null) {
            return "Sin estado";
        }

        switch (cita.getEstado()) {
            case PROGRAMADA:
                return "Programada";
            case CONFIRMADA:
                return "Confirmada";
            case ATENDIDA:
                return "Atendida";
            case CANCELADA:
                return "Cancelada";
            case NO_ASISTIO:
                return "No asistió";
            default:
                return cita.getEstado().toString();
        }
    }

    private String capitalizar(String texto) {
        if (texto == null || texto.isBlank()) {
            return texto;
        }
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private void cerrarVentana() {
        Stage stage = (Stage) btnCerrar.getScene().getWindow();
        stage.close();
    }
}
