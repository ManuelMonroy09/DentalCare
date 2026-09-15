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
        citaDialog.setContentText("");
        configurarDialogo(citaDialog, 600, 250);
        var citaResultado = citaDialog.showAndWait();
        if (citaResultado.isEmpty()) return;
        Cita cita = citaResultado.get().cita();

        BigDecimal anticipos = finanzasService.obtenerTotalAnticipos(cita.getId());
        TextInputDialog montoDialog = new TextInputDialog("0.00");
        montoDialog.setTitle("Registrar anticipo");
        montoDialog.setHeaderText("Anticipos registrados: " + moneda(anticipos));
        montoDialog.setContentText("Monto del anticipo:");
        configurarDialogo(montoDialog, 460, 220);
        var montoResultado = montoDialog.showAndWait();
        if (montoResultado.isEmpty()) return;

        BigDecimal monto;
        try { monto = new BigDecimal(montoResultado.get().trim().replace(",", ".")); }
        catch (NumberFormatException ex) { mensajeLabel.setText("El monto no tiene un formato válido."); return; }

        ChoiceDialog<MetodoPago> metodoDialog = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        metodoDialog.setTitle("Método de pago");
        metodoDialog.setHeaderText("Selecciona el método utilizado para el anticipo");
        metodoDialog.setContentText("Método:");
        configurarDialogo(metodoDialog, 460, 220);
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
            String fechaHora = cita.getInicio() == null ? "Fecha no disponible" : cita.getInicio().format(FECHA_HORA);
            return paciente + " · " + fechaHora;
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
        configurarDialogo(montoDialog, 460, 220);
        var montoResultado = montoDialog.showAndWait();
        if (montoResultado.isEmpty()) return;

        BigDecimal monto;
        try { monto = new BigDecimal(montoResultado.get().trim().replace(",", ".")); }
        catch (NumberFormatException ex) { mensajeLabel.setText("El monto no tiene un formato válido."); return; }

        ChoiceDialog<MetodoPago> metodoDialog = new ChoiceDialog<>(MetodoPago.EFECTIVO, MetodoPago.values());
        metodoDialog.setTitle("Método de pago");
        metodoDialog.setHeaderText("Selecciona el método utilizado");
        metodoDialog.setContentText("Método:");
        configurarDialogo(metodoDialog, 460, 220);
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

    private void configurarDialogo(Dialog<?> dialogo, double ancho, double alto) {
        DialogPane pane = dialogo.getDialogPane();
        pane.getStyleClass().add("standard-dialog");
        String dentalcareCss = getClass().getResource("/ui/css/dentalcare.css").toExternalForm();
        String dialogCss = getClass().getResource("/ui/css/dialog.css").toExternalForm();
        if (!pane.getStylesheets().contains(dentalcareCss)) pane.getStylesheets().add(dentalcareCss);
        if (!pane.getStylesheets().contains(dialogCss)) pane.getStylesheets().add(dialogCss);
        pane.setMinWidth(ancho);
        pane.setPrefWidth(ancho);
        pane.setMinHeight(alto);
        pane.setPrefHeight(alto);
        pane.applyCss();
        for (ButtonType tipo : pane.getButtonTypes()) {
            if (!(pane.lookupButton(tipo) instanceof Button button)) continue;
            button.getStyleClass().removeAll("dialog-primary-button", "dialog-secondary-button");
            if (tipo.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE || tipo == ButtonType.CANCEL || tipo == ButtonType.CLOSE) {
                button.getStyleClass().add("dialog-secondary-button");
            } else {
                button.getStyleClass().add("dialog-primary-button");
            }
            button.setMinHeight(40);
            button.setPrefHeight(40);
            button.setMaxHeight(44);
            button.setWrapText(false);
            button.setMnemonicParsing(false);
        }
        dialogo.setResizable(false);
    }

    private void ofrecerImpresion(Pago pago, Cargo cargo) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Pago registrado");
        alert.setHeaderText("Pago registrado correctamente");
        alert.setContentText("¿Deseas imprimir el recibo para el paciente?");
        ButtonType imprimir = new ButtonType("Ver recibo");
        ButtonType despues = new ButtonType("Ahora no", ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(imprimir, despues);
        configurarDialogo(alert, 460, 200);
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
        configurarDialogo(dialogo, 380, 620);
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
        agregarTexto(ticket, "Importe: " + moneda(pago.getMonto()), "-fx-font-size: 14px; -fx-font-weight: bold; -fx-padding: 8px 0 0 0;");
        agregarTexto(ticket, "Pendiente: " + moneda(pendiente), "-fx-font-size: 11px;");
        agregarTexto(ticket, "Gracias por su visita.", "-fx-font-size: 11px; -fx-alignment: center; -fx-padding: 12px 0 0 0;");
        return ticket;
    }

    private void imprimirTicket(VBox ticket) {
        PrinterJob job = PrinterJob.createPrinterJob();
        if (job == null) { mostrarError("Impresión", "No hay una impresora disponible."); return; }
        if (job.showPrintDialog(ticket.getScene().getWindow())) {
            boolean impreso = job.printPage(ticket);
            if (impreso) job.endJob(); else mostrarError("Impresión", "No fue posible enviar el recibo a la impresora.");
        }
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
        BigDecimal anticipos = finanzasService.obtenerTotalAnticipos(cargo.getId());
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Detalle financiero");
        alert.setHeaderText("Detalle del cargo");
        alert.setContentText("Paciente: " + nombrePaciente(cargo.getPacienteId()) + "\n" +
                "Concepto: " + valor(cargo.getConcepto()) + "\n" +
                "Cargo: " + moneda(cargo.getImporte()) + "\n" +
                "Anticipos: " + moneda(anticipos) + "\n" +
                "Pagado: " + moneda(pagado) + "\n" +
                "Pendiente: " + moneda(pendiente) + "\n" +
                "Estado: " + finanzasService.obtenerEstadoCargo(cargo.getId()).getDescripcion());
        configurarDialogo(alert, 520, 320);
        alert.showAndWait();
    }

    private void cargarPacientes() {
        pacientes.clear();
        for (Paciente paciente : pacientesService.obtenerTodos()) {
            if (paciente.getId() != null) pacientes.put(paciente.getId(), paciente);
        }
    }

    private void cargarDatos() {
        var cargos = finanzasService.obtenerCargos();
        cargosTable.setItems(FXCollections.observableArrayList(cargos));
        LocalDate hoy = LocalDate.now();
        BigDecimal ingresos = finanzasService.obtenerIngresos(hoy, hoy);
        BigDecimal pendiente = cargos.stream()
                .map(cargo -> finanzasService.obtenerSaldoPendiente(cargo.getId()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int cobrosHoy = finanzasService.obtenerCantidadPagos(hoy, hoy);
        ingresosLabel.setText(moneda(ingresos));
        pendienteLabel.setText(moneda(pendiente));
        cobrosHoyLabel.setText(String.valueOf(cobrosHoy));
    }

    private String nombrePaciente(Long id) {
        Paciente paciente = pacientes.get(id);
        if (paciente == null) return "Paciente #" + id;
        return (paciente.getNombre() + " " + paciente.getApellidoPaterno() + " " + paciente.getApellidoMaterno())
                .trim().replaceAll("\\s+", " ");
    }

    private String moneda(BigDecimal valor) {
        if (valor == null) return "$0.00";
        return "$" + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String valor(Object valor) { return valor == null ? "" : String.valueOf(valor); }
    private boolean vacio(String valor) { return valor == null || valor.isBlank(); }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(mensaje);
        configurarDialogo(alert, 460, 210);
        alert.showAndWait();
    }
}