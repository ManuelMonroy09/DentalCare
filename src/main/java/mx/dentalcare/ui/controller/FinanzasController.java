package mx.dentalcare.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.service.CitaService;
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
    @FXML private Label mensajeLabel;

    private final FinanzasService finanzasService;
    private final CitaService citaService;
    private final PacientesService pacientesService;
    private final Map<Long, Paciente> pacientes = new HashMap<>();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public FinanzasController(FinanzasService finanzasService, CitaService citaService, PacientesService pacientesService) {
        this.finanzasService = finanzasService;
        this.citaService = citaService;
        this.pacientesService = pacientesService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarPacientes();
        cargarDatos();
    }

    private void configurarTabla() {
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
        int creados = 0;
        for (Cita cita : citaService.obtenerHistorial()) {
            if (cita.getId() == null || cita.obtenerTotalTratamientos().compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            boolean existia = finanzasService.obtenerCargos().stream()
                    .anyMatch(c -> cita.getId().equals(c.getCitaId()));

            if (!existia) {
                finanzasService.obtenerOCrearCargo(cita);
                creados++;
            }
        }

        cargarDatos();
        mensajeLabel.setText(creados == 0
                ? "No hay nuevos cargos por generar."
                : creados + " cargo(s) generado(s).");
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
            monto = new BigDecimal(montoResultado.get().trim());
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
            cargosTable.getSelectionModel().select(cargo);
            mensajeLabel.setText("Pago registrado correctamente.");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    private void cargarDatos() {
        List<Cargo> cargos = finanzasService.obtenerCargos();
        cargosTable.setItems(FXCollections.observableArrayList(cargos));
        LocalDate hoy = LocalDate.now();
        ingresosLabel.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
        pendienteLabel.setText(moneda(finanzasService.obtenerPorCobrar()));
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
