package mx.dentalcare.ui.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.print.PrinterJob;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.domain.configuracion.ConfiguracionConsultorio;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.domain.financiero.EstadoCargo;
import mx.dentalcare.domain.financiero.MetodoPago;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.ConfiguracionService;
import mx.dentalcare.service.FinanzasService;
import mx.dentalcare.service.PacientesService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
    private final ConfiguracionService configuracionService;
    private final CitaService citaService;
    private final Map<Long, Paciente> pacientes = new HashMap<>();
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public FinanzasController(FinanzasService finanzasService, PacientesService pacientesService,
                              ConfiguracionService configuracionService, CitaService citaService) {
        this.finanzasService = finanzasService;
        this.pacientesService = pacientesService;
        this.configuracionService = configuracionService;
        this.citaService = citaService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        cargarPacientes();
        cargarDatos();
    }

    private void configurarTabla() {
        cargosTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        fechaColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFecha() == null ? "" : data.getValue().getFecha().format(FECHA)));
        pacienteColumn.setCellValueFactory(data -> new SimpleStringProperty(nombrePaciente(data.getValue().getPacienteId())));
        conceptoColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getConcepto()));
        importeColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(data.getValue().getImporte())));
        pagadoColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(finanzasService.obtenerTotalPagado(data.getValue().getId()))));
        pendienteColumn.setCellValueFactory(data -> new SimpleStringProperty(moneda(finanzasService.obtenerSaldoPendiente(data.getValue().getId()))));
        estadoColumn.setCellValueFactory(data -> new SimpleStringProperty(finanzasService.obtenerEstadoCargo(data.getValue().getId()).getDescripcion()));
    }

    @FXML
    private void sincronizarCargos() {
        try {
            int creados = finanzasService.generarCargosPendientes();
            cargarDatos();
            mensajeLabel.setText(creados == 0 ? "No hay nuevos cargos por generar." : creados + " cargo(s) sincronizado(s).");
        } catch (IllegalStateException | IllegalArgumentException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    @FXML
    private void registrarAnticipo() {
        List<Cita> citas = citaService.obtenerTodas().stream()
                .filter(c -> c.getEstado() == EstadoCita.PROGRAMADA || c.getEstado() == EstadoCita.CONFIRMADA)
                .filter(c -> c.getId() != null && c.getPaciente() != null && c.getPaciente().getId() != null)
                .sorted(Comparator.comparing(Cita::getInicio, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        if (citas.isEmpty()) {
            mensajeLabel.setText("No hay citas programadas o confirmadas disponibles para registrar un anticipo.");
            return;
        }

        List<CitaAnticipoOption> opciones = citas.stream().map(CitaAnticipoOption::new).toList();
        ChoiceDialog<CitaAnticipoOption> citaDialog = new ChoiceDialog<>(opciones.get(0), opciones);
        citaDialog.setTitle("Registrar anticipo");
        citaDialog.setHeaderText("Seleccionar cita para registrar el anticipo");
        citaDialog.setContentText(null);
        citaDialog.getDialogPane().setMinWidth(600);
        citaDialog.getDialogPane().setPrefWidth(600);
        citaDialog.getDialogPane().setMinHeight(190);
        citaDialog.getDialogPane().setPrefHeight(210);
        citaDialog.setResizable(false);
        var citaResultado = citaDialog.showAndWait();
        if (citaResultado.isEmpty()) return;
        Cita cita = citaResultado.get().cita();

        BigDecimal anticipos = finanzasService.obtenerTotalAnticipos(cita.getId());
        TextInputDialog montoDialog = new TextInputDialog("0.00");
        montoDialog.setTitle("Registrar anticipo");
        montoDialog.setHeaderText("Anticipos registrados: " + moneda(anticipos));
        montoDialog.setContentText("Monto del anticipo:");
        var montoResultado = montoDialog.showAndWait();
        if (montoResultado.isEmpty()) return;

        BigDecimal monto;
        try { monto = new BigDecimal(montoResultado.get().trim().replace(",", ".")); }
        catch (NumberFormatException ex) { mensajeLabel.setText("El monto no tiene un formato válido."); return; }

        ChoiceDialog<MetodoPago> metodoDialog = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        metodoDialog.setTitle("Método de pago");
        metodoDialog.setHeaderText("Selecciona el método utilizado para el anticipo");
        metodoDialog.setContentText("Método:");
        var metodoResultado = metodoDialog.showAndWait();
        if (metodoResultado.isEmpty()) return;

        try {
            finanzasService.registrarAnticipo(cita.getId(), monto, metodoResultado.get(), null);
            cargarDatos();
            mensajeLabel.setText("Anticipo registrado correctamente para " + nombrePaciente(cita.getPaciente().getId()) + ".");
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    private record CitaAnticipoOption(Cita cita) {
        @Override
        public String toString() {
            String paciente = cita.getPaciente() == null ? "Paciente" :
                    (cita.getPaciente().getNombre() + " " + cita.getPaciente().getApellidoPaterno() + " " + cita.getPaciente().getApellidoMaterno())
                            .trim().replaceAll("\\s+", " ");
            String fecha = cita.getInicio() == null ? "Fecha no disponible" : cita.getInicio().format(FECHA_HORA);
            String estado = cita.getEstado() == EstadoCita.CONFIRMADA ? "Confirmada" : "Programada";
            return paciente + " · " + fecha + " · " + estado;
        }
    }

    @FXML
    private void registrarPago() {
        Cargo cargo = cargosTable.getSelectionModel().getSelectedItem();
        if (cargo == null) { mensajeLabel.setText("Selecciona un cargo para registrar un pago."); return; }
        BigDecimal pendiente = finanzasService.obtenerSaldoPendiente(cargo.getId());
        if (pendiente.compareTo(BigDecimal.ZERO) <= 0) { mensajeLabel.setText("El cargo seleccionado ya está pagado."); return; }

        TextInputDialog montoDialog = new TextInputDialog(pendiente.toPlainString());
        montoDialog.setTitle("Registrar pago");
        montoDialog.setHeaderText("Saldo pendiente: " + moneda(pendiente));
        montoDialog.setContentText("Monto del pago:");
        var montoResultado = montoDialog.showAndWait();
        if (montoResultado.isEmpty()) return;

        BigDecimal monto;
        try { monto = new BigDecimal(montoResultado.get().trim().replace(",", ".")); }
        catch (NumberFormatException ex) { mensajeLabel.setText("El monto no tiene un formato válido."); return; }

        ChoiceDialog<MetodoPago> metodoDialog = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        metodoDialog.setTitle("Método de pago");
        metodoDialog.setHeaderText("Selecciona el método utilizado");
        metodoDialog.setContentText("Método:");
        var metodoResultado = metodoDialog.showAndWait();
        if (metodoResultado.isEmpty()) return;

        try {
            Pago pago = finanzasService.registrarPago(cargo.getId(), monto, metodoResultado.get(), null);
            cargarDatos();
            mensajeLabel.setText("Pago registrado correctamente.");
            ofrecerImpresion(pago, cargo);
        } catch (IllegalArgumentException | IllegalStateException ex) {
            mensajeLabel.setText(ex.getMessage());
        }
    }

    private void ofrecerImpresion(Pago pago, Cargo cargo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pago registrado");
        alert.setHeaderText("Pago registrado correctamente");
        alert.setContentText("¿Deseas imprimir el recibo para el paciente?");
        ButtonType imprimir = new ButtonType("Ver recibo");
        ButtonType despues = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(imprimir, despues);
        if (alert.showAndWait().orElse(despues) == imprimir) mostrarVistaPrevia(pago, cargo);
    }

    @FXML
    private void imprimirRecibo() {
        Cargo cargo = cargosTable.getSelectionModel().getSelectedItem();
        if (cargo == null) { mensajeLabel.setText("Selecciona un cargo para imprimir su recibo."); return; }
        List<Pago> pagos = finanzasService.obtenerPagosPorCargo(cargo.getId());
        if (pagos.isEmpty()) { mensajeLabel.setText("El cargo seleccionado no tiene pagos registrados."); return; }
        Pago ultimoPago = pagos.stream()
                .max(Comparator.comparing(Pago::getFecha, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(Pago::getId, Comparator.nullsFirst(Comparator.naturalOrder())))
                .orElse(pagos.get(pagos.size() - 1));
        mostrarVistaPrevia(ultimoPago, cargo);
    }

    private void mostrarVistaPrevia(Pago pago, Cargo cargo) {
        VBox ticket = construirTicket(pago, cargo);
        ScrollPane scroll = new ScrollPane(ticket);
        scroll.setFitToWidth(true);
        scroll.setFitToHeight(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: white;");
        Dialog<ButtonType> dialogo = new Dialog<>();
        dialogo.setTitle("Vista previa del recibo");
        dialogo.setHeaderText("Vista previa del recibo de pago");
        dialogo.getDialogPane().setContent(scroll);
        dialogo.getDialogPane().setPrefWidth(380);
        dialogo.getDialogPane().setPrefHeight(620);
        ButtonType imprimir = new ButtonType("Imprimir", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancelar = new ButtonType("Cerrar", ButtonBar.ButtonData.CANCEL_CLOSE);
        dialogo.getDialogPane().getButtonTypes().setAll(imprimir, cancelar);
        dialogo.showAndWait().ifPresent(resultado -> { if (resultado == imprimir) imprimirTicket(ticket); });
    }

    private VBox construirTicket(Pago pago, Cargo cargo) {
        ConfiguracionConsultorio configuracion = configuracionService.obtener();
        Paciente paciente = pacientes.get(cargo.getPacienteId());
        String nombre = paciente == null ? "Paciente #" + cargo.getPacienteId() : nombrePaciente(cargo.getPacienteId());
        BigDecimal pendiente = finanzasService.obtenerSaldoPendiente(cargo.getId());
        VBox ticket = new VBox(6);
        ticket.setPrefWidth(280); ticket.setMinWidth(280); ticket.setMaxWidth(280);
        ticket.setStyle("-fx-background-color: white; -fx-padding: 16px; -fx-font-family: 'Segoe UI';");
        agregarTexto(ticket, valor(configuracion.getNombreConsultorio()), "-fx-font-size: 18px; -fx-font-weight: bold; -fx-alignment: center;");
        if (!vacio(configuracion.getNombreOdontologo())) agregarTexto(ticket, configuracion.getNombreOdontologo(), "-fx-font-size: 12px; -fx-alignment: center;");
        if (!vacio(configuracion.getDireccion())) agregarTexto(ticket, configuracion.getDireccion(), "-fx-font-size: 11px; -fx-alignment: center;");
        if (!vacio(configuracion.getTelefono())) agregarTexto(ticket, "Tel. " + configuracion.getTelefono(), "-fx-font-size: 11px; -fx-alignment: center;");
        if (!vacio(configuracion.getEmail())) agregarTexto(ticket, configuracion.getEmail(), "-fx-font-size: 11px; -fx-alignment: center;");
        agregarTexto(ticket, "RECIBO DE PAGO", "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 0 4px 0;");
        agregarTexto(ticket, "Folio de pago: #" + valor(pago.getId()), "-fx-font-size: 11px;");
        agregarTexto(ticket, "Fecha: " + (pago.getFecha() == null ? "" : pago.getFecha().format(FECHA_HORA)), "-fx-font-size: 11px;");
        agregarTexto(ticket, "Paciente: " + nombre, "-fx-font-size: 11px;");
        agregarTexto(ticket, "Concepto: " + valor(cargo.getConcepto()), "-fx-font-size: 11px;");
        agregarTexto(ticket, "Método: " + (pago.getMetodoPago() == null ? "" : pago.getMetodoPago().getDescripcion()), "-fx-font-size: 11px;");
        agregarTexto(ticket, "--------------------------------", "-fx-font-size: 10px;");
        agregarTexto(ticket, "Cargo total:  " + moneda(cargo.getImporte()), "-fx-font-size: 11px;");
        agregarTexto(ticket, "Pago recibido: " + moneda(pago.getMonto()), "-fx-font-size: 13px; -fx-font-weight: bold;");
        agregarTexto(ticket, "Saldo pendiente: " + moneda(pendiente), "-fx-font-size: 11px;");
        if (!vacio(configuracion.getPieRecibo())) agregarTexto(ticket, configuracion.getPieRecibo(), "-fx-font-size: 11px; -fx-padding: 10px 0 0 0;");
        return ticket;
    }

    private void imprimirTicket(VBox ticket) {
        ticket.applyCss(); ticket.autosize(); ticket.layout();
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { mostrarError("Impresión", "No hay una impresora disponible en el sistema."); return; }
        if (!job.showPrintDialog(cargosTable.getScene().getWindow())) return;
        boolean impreso = job.printPage(ticket);
        if (impreso) job.endJob(); else mostrarError("Impresión", "No fue posible enviar el recibo a la impresora.");
    }

    private void agregarTexto(VBox contenedor, String texto, String estilo) {
        Label label = new Label(texto); label.setWrapText(true); label.setMaxWidth(Double.MAX_VALUE); label.setStyle(estilo); contenedor.getChildren().add(label);
    }

    @FXML
    private void verDetalle() {
        Cargo cargo = cargosTable.getSelectionModel().getSelectedItem();
        if (cargo == null) { mensajeLabel.setText("Selecciona un cargo para ver su detalle."); return; }
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
                .append("Historial de pagos\n");
        if (pagos.isEmpty()) detalle.append("Sin pagos registrados.");
        else for (Pago pago : pagos) {
            detalle.append("• ").append(pago.getFecha() == null ? "" : pago.getFecha().format(FECHA_HORA)).append(" | ")
                    .append(moneda(pago.getMonto())).append(" | ")
                    .append(pago.getMetodoPago() == null ? "Sin método" : pago.getMetodoPago().getDescripcion()).append("\n");
            if (pago.getNotas() != null && !pago.getNotas().isBlank()) detalle.append("  Nota: ").append(pago.getNotas()).append("\n");
        }
        Alert dialogo = new Alert(Alert.AlertType.INFORMATION);
        dialogo.setTitle("Detalle del cargo"); dialogo.setHeaderText("Cargo #" + cargo.getId()); dialogo.setContentText(detalle.toString());
        dialogo.getDialogPane().setMinWidth(520); dialogo.showAndWait();
    }

    private void cargarDatos() {
        finanzasService.generarCargosPendientes();
        cargosTable.setItems(FXCollections.observableArrayList(finanzasService.obtenerCargos()));
        LocalDate hoy = LocalDate.now();
        ingresosLabel.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
        pendienteLabel.setText(moneda(finanzasService.obtenerPorCobrar()));
        cobrosHoyLabel.setText(String.valueOf(finanzasService.obtenerCantidadPagos(hoy, hoy)));
    }

    private void cargarPacientes() {
        pacientes.clear();
        pacientesService.obtenerTodos().forEach(p -> { if (p.getId() != null) pacientes.put(p.getId(), p); });
    }

    private String nombrePaciente(Long id) {
        Paciente p = pacientes.get(id);
        if (p == null) return "Paciente #" + (id == null ? "?" : id);
        return (p.getNombre() + " " + p.getApellidoPaterno() + " " + p.getApellidoMaterno()).trim().replaceAll("\\s+", " ");
    }

    private String moneda(BigDecimal valor) { return "$" + (valor == null ? "0.00" : valor.setScale(2, RoundingMode.HALF_UP).toPlainString()); }
    private String valor(Object valor) { return valor == null ? "" : String.valueOf(valor); }
    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo); alert.setHeaderText(titulo); alert.setContentText(mensaje); alert.showAndWait();
    }
}