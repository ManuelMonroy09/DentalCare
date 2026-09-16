package mx.dentalcare.ui.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.tratamiento.TratamientoAplicado;
import mx.dentalcare.service.CitaService;
import mx.dentalcare.service.FinanzasService;
import mx.dentalcare.service.PacientesService;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class DashboardController {

    @FXML private Label lblTotalPacientes;
    @FXML private Label lblCitasHoy;
    @FXML private Label lblProximaHora;
    @FXML private Label lblProximoPaciente;
    @FXML private Label lblProximoTratamiento;
    @FXML private Label lblProximoEstado;
    @FXML private Label lblIngresosHoy;
    @FXML private Label lblPorCobrar;
    @FXML private Label lblCobrosHoy;

    private final PacientesService pacienteService;
    private final CitaService citaService;
    private final FinanzasService finanzasService;

    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");

    public DashboardController(PacientesService pacienteService, CitaService citaService,
                               FinanzasService finanzasService) {
        this.pacienteService = pacienteService;
        this.citaService = citaService;
        this.finanzasService = finanzasService;
    }

    @FXML
    public void initialize() {
        cargarResumen();
        cargarProximaCita();
        cargarResumenFinanciero();
        cargarTarjetasComplementarias();
    }

    private void cargarResumen() {
        int totalPacientes = pacienteService.obtenerTodos().size();
        long citasHoy = citaService.obtenerTodas().stream()
                .filter(cita -> cita.getInicio() != null)
                .filter(cita -> cita.getInicio().toLocalDate().equals(LocalDate.now()))
                .count();
        lblTotalPacientes.setText(String.valueOf(totalPacientes));
        lblCitasHoy.setText(String.valueOf(citasHoy));
    }

    private void cargarResumenFinanciero() {
        LocalDate hoy = LocalDate.now();
        lblIngresosHoy.setText(moneda(finanzasService.obtenerIngresos(hoy, hoy)));
        lblPorCobrar.setText(moneda(finanzasService.obtenerPorCobrar()));
        lblCobrosHoy.setText(String.valueOf(finanzasService.obtenerCantidadPagos(hoy, hoy)));
    }

    private void cargarProximaCita() {
        LocalDateTime ahora = LocalDateTime.now();
        LocalDate hoy = LocalDate.now();

        Cita proximaCita = citaService.obtenerTodas().stream()
                .filter(cita -> cita.getInicio() != null)
                .filter(cita -> cita.getInicio().toLocalDate().equals(hoy))
                .filter(cita -> cita.getInicio().isAfter(ahora))
                .min(Comparator.comparing(Cita::getInicio))
                .orElse(null);

        if (proximaCita == null) {
            mostrarSinProximaCita();
            return;
        }

        lblProximaHora.setText(proximaCita.getInicio().format(FORMATO_HORA));
        if (proximaCita.getPaciente() != null) {
            lblProximoPaciente.setText(proximaCita.getPaciente().getNombre() + " "
                    + proximaCita.getPaciente().getApellidoPaterno());
        } else {
            lblProximoPaciente.setText("Paciente no disponible");
        }
        lblProximoTratamiento.setText(obtenerResumenTratamientos(proximaCita));
        lblProximoEstado.setText(proximaCita.getEstado() != null ? proximaCita.getEstado().name() : "SIN ESTADO");
    }

    private void mostrarSinProximaCita() {
        lblProximaHora.setText("--:--");
        lblProximoPaciente.setText("No hay próximas citas");
        lblProximoTratamiento.setText("Agenda libre");
        lblProximoEstado.setText("");
    }

    private String obtenerResumenTratamientos(Cita cita) {
        if (cita.getTratamientos() == null || cita.getTratamientos().isEmpty()) return "Sin tratamiento registrado";
        List<String> nombres = cita.getTratamientos().stream()
                .filter(tratamiento -> tratamiento != null)
                .map(TratamientoAplicado::getNombre)
                .filter(nombre -> nombre != null && !nombre.isBlank())
                .toList();
        if (nombres.isEmpty()) return "Sin tratamiento registrado";
        if (nombres.size() == 1) return nombres.get(0);
        return nombres.get(0) + " +" + (nombres.size() - 1);
    }

    /**
     * Las tarjetas adicionales se construyen fuera del FXML estable de Inicio.
     * Se agregan en una sección independiente debajo de "Resumen de hoy".
     * No realizan consultas nuevas ni cambian la lógica existente.
     */
    private void cargarTarjetasComplementarias() {
        try {
            VBox resumenGeneral = obtenerSeccionPrincipal();
            if (resumenGeneral == null || resumenGeneral.getParent() == null) return;

            VBox pagina = (VBox) resumenGeneral.getParent();

            // El contenido existente conserva su diseño, pero deja de intentar
            // ocupar todo el alto disponible para permitir que la nueva sección
            // se acomode dentro de la misma pantalla.
            VBox.setVgrow(resumenGeneral, Priority.NEVER);
            resumenGeneral.setSpacing(12);

            VBox nuevaSeccion = new VBox(8);
            nuevaSeccion.getStyleClass().add("standard-section");
            nuevaSeccion.setPrefHeight(128);
            nuevaSeccion.setMinHeight(128);
            nuevaSeccion.setMaxHeight(128);

            Label titulo = new Label("Información adicional");
            titulo.getStyleClass().add("standard-section-title");

            HBox contenedor = new HBox(12);
            contenedor.setFillHeight(true);
            VBox.setVgrow(contenedor, Priority.ALWAYS);

            VBox agenda = crearTarjetaAgenda();
            VBox cuentas = crearTarjetaCuentas();

            HBox.setHgrow(agenda, Priority.ALWAYS);
            HBox.setHgrow(cuentas, Priority.ALWAYS);

            contenedor.getChildren().addAll(agenda, cuentas);
            nuevaSeccion.getChildren().addAll(titulo, contenedor);

            VBox.setVgrow(nuevaSeccion, Priority.NEVER);
            pagina.getChildren().add(nuevaSeccion);
        } catch (Exception e) {
            System.err.println("No fue posible cargar las tarjetas complementarias de Inicio: " + e.getMessage());
        }
    }

    private VBox obtenerSeccionPrincipal() {
        if (lblIngresosHoy == null || lblIngresosHoy.getParent() == null
                || lblIngresosHoy.getParent().getParent() == null
                || lblIngresosHoy.getParent().getParent().getParent() == null
                || lblIngresosHoy.getParent().getParent().getParent().getParent() == null) {
            return null;
        }

        if (lblIngresosHoy.getParent().getParent().getParent() instanceof VBox resumenGeneral) {
            return resumenGeneral;
        }
        return null;
    }

    private VBox crearTarjetaAgenda() {
        VBox tarjeta = new VBox(8);
        tarjeta.getStyleClass().add("card");
        tarjeta.setPrefHeight(84);
        tarjeta.setMinHeight(84);
        tarjeta.setMaxHeight(84);
        tarjeta.setStyle("-fx-padding: 12px;");

        Label titulo = new Label("Agenda de la semana");
        titulo.getStyleClass().add("card-title");

        HBox dias = new HBox(8);
        dias.setFillHeight(true);
        for (String dia : new String[]{"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"}) {
            VBox columna = new VBox(3);
            columna.setFillWidth(true);
            columna.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
            HBox.setHgrow(columna, Priority.ALWAYS);

            Label nombre = new Label(dia);
            nombre.getStyleClass().add("dashboard-summary-label");
            Label cantidad = new Label("0");
            cantidad.getStyleClass().add("dashboard-summary-value");
            columna.getChildren().addAll(nombre, cantidad);
            dias.getChildren().add(columna);
        }

        VBox.setVgrow(dias, Priority.ALWAYS);
        tarjeta.getChildren().addAll(titulo, dias);
        return tarjeta;
    }

    private VBox crearTarjetaCuentas() {
        VBox tarjeta = new VBox(6);
        tarjeta.getStyleClass().add("card");
        tarjeta.setPrefHeight(84);
        tarjeta.setMinHeight(84);
        tarjeta.setMaxHeight(84);
        tarjeta.setStyle("-fx-padding: 12px;");

        Label titulo = new Label("Estado de cuentas");
        titulo.getStyleClass().add("card-title");

        HBox datos = new HBox(18);
        datos.setFillHeight(true);

        datos.getChildren().addAll(
                crearDatoCuenta("Por cobrar", "$0.00"),
                crearDatoCuenta("Cargos pendientes", "0"),
                crearDatoCuenta("Cargos registrados", "0")
        );

        for (var dato : datos.getChildren()) {
            HBox.setHgrow(dato, Priority.ALWAYS);
        }

        tarjeta.getChildren().addAll(titulo, datos);
        return tarjeta;
    }

    private VBox crearDatoCuenta(String nombre, String valor) {
        VBox dato = new VBox(2);
        Label etiqueta = new Label(nombre);
        etiqueta.getStyleClass().add("dashboard-summary-label");
        Label cantidad = new Label(valor);
        cantidad.getStyleClass().add("dashboard-summary-value");
        dato.getChildren().addAll(etiqueta, cantidad);
        return dato;
    }

    private String moneda(BigDecimal valor) {
        return "$" + (valor == null ? "0.00" : valor.setScale(2).toPlainString());
    }
}
