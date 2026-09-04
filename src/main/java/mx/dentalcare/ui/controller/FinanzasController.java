package mx.dentalcare.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.service.FinanzasService;
import mx.dentalcare.service.PacientesService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class FinanzasController {
    @FXML private TableView<Cargo> cargosTable;
    @FXML private TableColumn<Cargo, String> fechaColumn;
    @FXML private TableColumn<Cargo, String> pacienteColumn;
    @FXML private TableColumn<Cargo, String> conceptoColumn;
    @FXML private TableColumn<Cargo, String> importeColumn;
    @FXML private TableColumn<Cargo, String> pagadoColumn;
    @FXML private TableColumn<Cargo, String> pendienteColumn;
    @FXML private TableColumn<Cargo, String> estadoColumn;
    @FXML private Label ingresosLabel;
    @FXML private Label pendienteLabel;
    @FXML private Label cobrosHoyLabel;
    @FXML private Label mensajeLabel;

    private final FinanzasService finanzasService;
    private final PacientesService pacientesService;
    private final Map<Long, Paciente> pacientes = new HashMap<>();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FinanzasController(FinanzasService finanzasService, PacientesService pacientesService) {
        this.finanzasService = finanzasService;
        this.pacientesService = pacientesService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarPacientes();
        cargarDatos();
    }

    private void configurarTabla() {
        // Hace que las columnas ocupen todo el ancho disponible sin dejar una franja vacía.
        cargosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        fechaColumn.setCellValueFactory(data -> new SimpleStringProperty(
                data.getValue().getFecha() == null ? "" : data.getValue().getFecha().format(FECHA)));
        pacienteColumn.setCellValueFactory(data -> new SimpleStringProperty(nombrePaciente(data.getValue().getPacienteId())));
        conceptoColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcepto()));
        importeColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(data.getValue().getImporte())));
        pagadoColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(finanzasService.obtenerTotalPagado(data.getValue().getId()))));
        pendienteColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(finanzasService.obtenerSaldoPendiente(data.getValue().getId()))));
        estadoColumn.setCellValueFactory(data -> new SimpleStringProperty(
                finanzasService.obtenerEstadoCargo(data.getValue().getId()).getDescripcion()));
    }

    @FXML
    private void sincronizarCargos() {
        try {
            int creados = finanzasService.generarCargosPendientes();
            cargarDatos();
            mensajeLabel.setText(creados == 0
                    ? "No hay nuevos cargos por generar."
                    : creados + " cargo(s) sincronizado(s).");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void registrarPago() {
        Cargo cargo = cargosTable.getSelectionModel().getSelectedItem();
        if (cargo == null) {
            mensajeLabel.setText("Selecciona un cargo para registrar un pago.");
            return;
        }

        BigDecimal pendiente = finanzasService.obtenerSaldoPendiente(cargo.getId());
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) {
            mensajeLabel.setText("El cargo seleccionado ya está pagado.");
            return;
        }

        TextInputDialog montoDialog = new TextInputDialog(pendiente.toPlainString());
        montoDialog.setTitle("Registrar pago");
        montoDialog.setHeaderText("Saldo pendiente: " + moneda(pendiente));
        montoDialog.setContentText("Monto del pago:");
        var montoResultado = montoDialog.showAndWait();
        if (montoResultado.isEmpty()) return;

        BigDecimal monto;
        try {
            monto = new BigDecimal(montoResultado.get().trim().replace(",", "."));
        } catch (NumberFormatException ex) {
            mensajeLabel.setText("El monto no tiene un formato válido.");
            return;
        }

        ChoiceDialog<MetodoPago> metodoDialog = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        metodoDialog.setTitle("Método de pago");
        metodoDialog.setHeaderText("Selecciona el método utilizado");
        metodoDialog.setContentText("Método:");
        var metodoResultado = metodoDialog.showAndWait();
        if (metodoResultado.isEmpty()) return;

        try {
            finanzasService.registrarPago(cargo.getId(), monto, metodoResultado.get(), null);
            cargarDatos();
            mensajeLabel.setText("Pago registrado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void verDetalle() {
        Cargo cargo = cargosTable.getSelectionModel().getSelectedItem();
        if (cargo == null) {
            mensajeLabel.setText("Selecciona un cargo para ver su detalle.");
            return;
        }

        BigDecimal pagado = finanzasService.obtenerTotalPagado(cargo.getId());
        BigDecimal pendiente = finanzasService.obtenerSaldoPendiente(cargo.getId());
        EstadoCargo estado = finanzasService.obtenerEstadoCargo(cargo.getId());
        List<Pago> pagos = finanzasService.obtenerPagosPorCargo(cargo.getId());

        StringBuilder detalle = new StringBuilder();
        detalle.append("Paciente: ").append(nombrePaciente(cargo.getPacienteId())).append("\n")
                .append("Fecha: ").append(cargo.getFecha() == null ? "" : cargo.getFecha().format(FECHA_HORA)).append("\n")
                .append("Concepto: ").append(cargo.getConcepto()).append("\n")
                .append("Importe total: ").append(moneda(cargo.getImporte())).append("\n")
                .append("Total pagado: ").append(moneda(pagado)).append("\n")
                .append("Saldo pendiente: ").append(moneda(pendiente)).append("\n")
                .append("Estado: ").append(estado.getDescripcion()).append("\n\n")
                .append("Historial de pagos").append("\n");

        if (pagos.isEmpty()) {
            detalle.append("Sin pagos registrados.");
        } else {
            for (Pago pago : pagos) {
                detalle.append("• ")
                        .append(pago.getFecha() == null ? "" : pago.getFecha().format(FECHA_HORA))
                        .append(" | ").append(moneda(pago.getMonto()))
                        .append(" | ").append(pago.getMetodoPago() == null ? "Sin método" : pago.getMetodoPago().getDescripcion())
                        .append("\n");
                if (pago.getNotas() != null && !pago.getNotas().isBlank()) {
                    detalle.append("  Nota: ").append(pago.getNotas()).append("\n");
                }
            }
        }

        Alert dialogo = new Alert(Alert.AlertType.INFORMATION);
        dialogo.setTitle("Detalle del cargo");
        dialogo.setHeaderText("Cargo #" + cargo.getId());
        dialogo.setContentText(detalle.toString());
        dialogo.getDialogPane().setMinWidth(520);
        dialogo.showAndWait();
    }

    private void cargarDatos() {
        // La sincronización automática conserva la reconciliación de cargos antiguos o faltantes.
        finanzasService.generarCargosPendientes();
        List<Cargo> cargos = finanzasService.obtenerCargos();
        cargosTable.setItems(FXCollections.observableArrayList(cargos));
        LocalDate hoy = LocalDate.now();
        ingresosLabel.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
        pendienteLabel.setText(moneda(finanzasService.obtenerPorCobrar()));
        cobrosHoyLabel.setText(String.valueOf(finanzasService.obtenerCantidadPagos(hoy, hoy)));
    }

    private void cargarPacientes() {
        pacientes.clear();
        pacientesService.obtenerTodos().forEach(p -> {
            if (p.getId() != null) pacientes.put(p.getId(), p);
        });
    }

    private String nombrePaciente(Long id) {
        Paciente p = pacientes.get(id);
        if (p == null) return "Paciente #" + (id == null ? "?" : id);
        return (p.getNombre() + " " + p.getApellidoPaterno() + " " + p.getApellidoMaterno())
                .trim().replaceAll("\\s+", " ");
    }

    private String moneda(BigDecimal valor) {
        return "$" + (valor == null ? "0.00" : valor.setScale(2).toPlainString());
    }
}
