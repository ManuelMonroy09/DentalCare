package mx.dentalcare.ui.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import mx.dentalcare.domain.auditoria.AuditEntry;
import mx.dentalcare.service.AuditService;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class AuditoriaController {

    @FXML private ComboBox<String> usuarioFiltro;
    @FXML private ComboBox<String> moduloFiltro;
    @FXML private ComboBox<String> accionFiltro;
    @FXML private DatePicker desdeFiltro;
    @FXML private DatePicker hastaFiltro;
    @FXML private TableView<AuditEntry> tabla;
    @FXML private Label detalleUsuario;
    @FXML private Label detalleRol;
    @FXML private Label detalleFecha;
    @FXML private Label detalleModulo;
    @FXML private Label detalleAccion;
    @FXML private Label detalleEntidad;
    @FXML private Label detalleResultado;
    @FXML private Label detalleDescripcion;
    @FXML private Label detalleAnterior;
    @FXML private Label detalleNuevo;

    private final AuditService auditService;
    private List<AuditEntry> entradas = List.of();
    private static final DateTimeFormatter FORMATO = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public AuditoriaController(AuditService auditService) {
        this.auditService = auditService;
    }

    @FXML
    public void initialize() {
        configurarTabla();
        tabla.getSelectionModel().selectedItemProperty().addListener((obs, anterior, actual) -> mostrarDetalle(actual));
        cargar();
    }

    @FXML
    private void aplicarFiltros() {
        tabla.setItems(FXCollections.observableArrayList(entradas.stream().filter(this::cumpleFiltros).toList()));
        limpiarDetalle();
    }

    @FXML
    private void limpiarFiltros() {
        usuarioFiltro.setValue("Todos");
        moduloFiltro.setValue("Todos");
        accionFiltro.setValue("Todas");
        desdeFiltro.setValue(null);
        hastaFiltro.setValue(null);
        aplicarFiltros();
    }

    private void cargar() {
        try {
            entradas = auditService.obtenerTodos();
            llenarFiltros();
            aplicarFiltros();
        } catch (Exception e) {
            entradas = List.of();
            tabla.setItems(FXCollections.observableArrayList());
            limpiarDetalle();
        }
    }

    private void llenarFiltros() {
        String usuarioActual = usuarioFiltro.getValue();
        String moduloActual = moduloFiltro.getValue();
        String accionActual = accionFiltro.getValue();

        usuarioFiltro.setItems(FXCollections.observableArrayList(
                "Todos", entradas.stream().map(AuditEntry::getDisplayName).filter(Objects::nonNull).distinct().sorted().toList()));
        moduloFiltro.setItems(FXCollections.observableArrayList(
                "Todos", entradas.stream().map(AuditEntry::getModulo).filter(Objects::nonNull).distinct().sorted().toList()));
        accionFiltro.setItems(FXCollections.observableArrayList(
                "Todas", entradas.stream().map(AuditEntry::getAccion).filter(Objects::nonNull).distinct().sorted().toList()));

        usuarioFiltro.setValue(usuarioActual == null ? "Todos" : usuarioActual);
        moduloFiltro.setValue(moduloActual == null ? "Todos" : moduloActual);
        accionFiltro.setValue(accionActual == null ? "Todas" : accionActual);
    }

    private void configurarTabla() {
        TableColumn<AuditEntry, String> fecha = new TableColumn<>("Fecha y hora");
        fecha.setCellValueFactory(c -> new ReadOnlyStringWrapper(formatear(c.getValue().getFechaHora())));
        TableColumn<AuditEntry, String> usuario = new TableColumn<>("Usuario");
        usuario.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getDisplayName()));
        TableColumn<AuditEntry, String> accion = new TableColumn<>("Acción");
        accion.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getAccion()));
        TableColumn<AuditEntry, String> modulo = new TableColumn<>("Módulo");
        modulo.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getModulo()));
        TableColumn<AuditEntry, String> entidad = new TableColumn<>("Registro");
        entidad.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getEntidadId() == null ? c.getValue().getEntidad() : c.getValue().getEntidad() + " #" + c.getValue().getEntidadId()));
        TableColumn<AuditEntry, String> resultado = new TableColumn<>("Resultado");
        resultado.setCellValueFactory(c -> new ReadOnlyStringWrapper(c.getValue().getResultado()));
        tabla.getColumns().setAll(fecha, usuario, accion, modulo, entidad, resultado);
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private boolean cumpleFiltros(AuditEntry e) {
        String usuario = usuarioFiltro.getValue();
        String modulo = moduloFiltro.getValue();
        String accion = accionFiltro.getValue();
        LocalDate desde = desdeFiltro.getValue();
        LocalDate hasta = hastaFiltro.getValue();
        if (usuario != null && !"Todos".equals(usuario) && !usuario.equals(e.getDisplayName())) return false;
        if (modulo != null && !"Todos".equals(modulo) && !modulo.equals(e.getModulo())) return false;
        if (accion != null && !"Todas".equals(accion) && !accion.equals(e.getAccion())) return false;
        LocalDateTime fecha = e.getFechaHora();
        if (desde != null && (fecha == null || fecha.toLocalDate().isBefore(desde))) return false;
        return hasta == null || (fecha != null && !fecha.toLocalDate().isAfter(hasta));
    }

    private void mostrarDetalle(AuditEntry e) {
        if (e == null) { limpiarDetalle(); return; }
        detalleUsuario.setText(e.getDisplayName() + " (" + e.getUsername() + ")");
        detalleRol.setText(e.getRole());
        detalleFecha.setText(formatear(e.getFechaHora()));
        detalleModulo.setText(e.getModulo());
        detalleAccion.setText(e.getAccion());
        detalleEntidad.setText(e.getEntidadId() == null ? e.getEntidad() : e.getEntidad() + " #" + e.getEntidadId());
        detalleResultado.setText(e.getResultado());
        detalleDescripcion.setText(e.getDescripcion());
        detalleAnterior.setText(e.getValorAnterior());
        detalleNuevo.setText(e.getValorNuevo());
    }

    private void limpiarDetalle() {
        detalleUsuario.setText("-"); detalleRol.setText("-"); detalleFecha.setText("-"); detalleModulo.setText("-");
        detalleAccion.setText("-"); detalleEntidad.setText("-"); detalleResultado.setText("-"); detalleDescripcion.setText("-");
        detalleAnterior.setText("-"); detalleNuevo.setText("-");
    }

    private String formatear(LocalDateTime fecha) { return fecha == null ? "-" : FORMATO.format(fecha); }
}
