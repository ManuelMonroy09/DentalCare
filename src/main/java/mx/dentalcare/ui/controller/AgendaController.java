package mx.dentalcare.ui.controller;

import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.domain.cita.EstadoCita;
import mx.dentalcare.service.CitaService;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Locale;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.Cursor;

@Component
public class AgendaController {

    @FXML
    private Button btnAnterior;

    @FXML
    private Button btnHoy;

    @FXML
    private Button btnSiguiente;

    @FXML
    private Label lblRangoFecha;

    @FXML
    private ComboBox<String> cmbVista;

    @FXML
    private Button btnNuevaCita;

    @FXML
    private ScrollPane scrollAgenda;

    @FXML
    private VBox contenedorAgenda;

    @FXML
    private TextField txtBuscarPaciente;

    private final CitaService citaService;
    private final ApplicationContext applicationContext;
    private LocalDate fechaActual;
    private Pane indicadorHoraActual;
    private AnchorPane columnaHoraActual;
    private VBox citaArrastrada;
    private Cita citaEnArrastre;
    private AnchorPane columnaOriginal;
    private AnchorPane columnaDestino;
    private double posicionOriginal;
    private double offsetYArrastre;
    private LocalDate fechaOriginal;
    private LocalDate fechaPropuestaArrastre;
    private LocalTime horaPropuestaArrastre;
    private LocalDateTime inicioOriginal;
    private boolean arrastrandoCita = false;
    private boolean arrastreValido = false;
    private static final double UMBRAL_ARRASTRE = 5.0;
    private boolean redimensionandoCita = false;
    private Cita citaRedimensionada;
    private VBox bloqueRedimensionado;
    private double alturaOriginal;
    private double alturaInicialResize;
    private LocalDateTime inicioOriginalResize;
    private LocalDateTime finOriginalResize;
    private LocalDateTime finPropuestoResize;
    private boolean resizeValido = false;
    private static final double ZONA_RESIZE = 10.0;
    private Timeline relojHoraActual;
    private static final int HORA_INICIO = 8;
    private static final int HORA_FIN = 20;
    private static final double ALTURA_POR_HORA = 80.0;
    private static final double ANCHO_HORA = 65.0;
    private static final double ANCHO_DIA_MINIMO = 120.0;
    private static final int NUMERO_DIAS = 7;
    private static final int NUMERO_DIAS_VISTA_DIA = 1;
    private static final DateTimeFormatter FORMATO_HORA = DateTimeFormatter.ofPattern("HH:mm");
    private static final Locale LOCALE_ES = new Locale("es", "MX");
    private static final int MAX_CITAS_VISIBLES_MES = 4;

    public AgendaController(CitaService citaService, ApplicationContext applicationContext) {
        this.citaService = citaService;
        this.applicationContext = applicationContext;
    }

    @FXML
    public void initialize() {
        fechaActual = LocalDate.now();
        configurarSelectorVista();
        configurarEventos();
        configurarRedimensionamiento();
        configurarRelojHoraActual();
        actualizarAgenda();
        configurarBusqueda();
    }

    private void configurarSelectorVista() {
        cmbVista.getItems().clear();
        cmbVista.getItems().addAll("Semana", "Día", "Mes");
        cmbVista.getSelectionModel().select("Semana");
        cmbVista.setOnAction(event -> {actualizarAgenda();});
    }

    private void configurarEventos() {
        btnAnterior.setOnAction(event -> {String vista = cmbVista.getSelectionModel().getSelectedItem();
            if ("Día".equals(vista)) {
                fechaActual = fechaActual.minusDays(1);
            } else if ("Mes".equals(vista)) {
                fechaActual = fechaActual.minusMonths(1);
            } else {
                fechaActual = fechaActual.minusWeeks(1);
            }
            actualizarAgenda();
        });
        btnHoy.setOnAction(event -> {fechaActual = LocalDate.now();actualizarAgenda();});
        btnSiguiente.setOnAction(event -> {String vista = cmbVista.getSelectionModel().getSelectedItem();
            if ("Día".equals(vista)) {
                fechaActual = fechaActual.plusDays(1);
            } else if ("Mes".equals(vista)) {
                fechaActual = fechaActual.plusMonths(1);
            } else {
                fechaActual = fechaActual.plusWeeks(1);
            }
            actualizarAgenda();
        });
        btnNuevaCita.setOnAction(event -> abrirNuevaCita());
    }

    private void configurarRelojHoraActual() {
        actualizarIndicadorHoraActual();
        relojHoraActual = new Timeline(new KeyFrame(Duration.seconds(1), event -> actualizarIndicadorHoraActual()));
        relojHoraActual.setCycleCount(Timeline.INDEFINITE);
        relojHoraActual.play();
    }

    private void actualizarIndicadorHoraActual() {
        if (indicadorHoraActual == null || columnaHoraActual == null) {
            return;
        }
        LocalDate hoy = LocalDate.now();
        LocalTime ahora = LocalTime.now();
        String vista = cmbVista.getSelectionModel().getSelectedItem();
        boolean mostrar;
        if ("Día".equals(vista)) {
            mostrar = fechaActual.equals(hoy);
        } else {
            LocalDate inicioSemana = obtenerInicioSemana(fechaActual);
            LocalDate finSemana = inicioSemana.plusDays(6);
            mostrar = !hoy.isBefore(inicioSemana) && !hoy.isAfter(finSemana);
        }
        LocalTime inicio = LocalTime.of(HORA_INICIO, 0);
        LocalTime fin = LocalTime.of(HORA_FIN, 0);
        if (!mostrar || ahora.isBefore(inicio) || ahora.isAfter(fin)) {
            indicadorHoraActual.setVisible(false);
            indicadorHoraActual.setManaged(false);
            return;
        }
        double minutosDesdeInicio = (ahora.getHour() - HORA_INICIO) * 60 + ahora.getMinute() + ahora.getSecond() / 60.0;
        double posicion = minutosDesdeInicio * ALTURA_POR_HORA / 60.0;
        AnchorPane.setTopAnchor(indicadorHoraActual, posicion);
        indicadorHoraActual.setVisible(true);
        indicadorHoraActual.setManaged(true);
    }

    private void configurarRedimensionamiento() {
        scrollAgenda.viewportBoundsProperty().addListener((observable, anterior, actual) -> ajustarAnchoAgenda());
        scrollAgenda.widthProperty().addListener((observable, anterior, actual) -> ajustarAnchoAgenda());
        contenedorAgenda.widthProperty().addListener((observable, anterior, actual) -> ajustarAnchoAgenda());
    }

    private void ajustarAnchoAgenda() {
        if (contenedorAgenda.getChildren().size() < 2) {
            return;
        }
        double anchoViewport = scrollAgenda.getViewportBounds().getWidth();
        if (anchoViewport <= 0) {
            return;
        }
        String vista = cmbVista.getSelectionModel().getSelectedItem();
        int numeroDias = "Día".equals(vista) ? NUMERO_DIAS_VISTA_DIA : NUMERO_DIAS;
        double anchoMinimoDias = ANCHO_DIA_MINIMO * numeroDias;
        double anchoDisponibleDias = anchoViewport - ANCHO_HORA;
        double anchoDia;
        if (anchoDisponibleDias >= anchoMinimoDias) {
            anchoDia = anchoDisponibleDias / numeroDias;
        } else {
            anchoDia = ANCHO_DIA_MINIMO;
        }
        double anchoTotal = ANCHO_HORA + (anchoDia * numeroDias);
        if (contenedorAgenda.getChildren().get(0) instanceof HBox) {
            HBox encabezado = (HBox) contenedorAgenda.getChildren().get(0);
            encabezado.setPrefWidth(anchoTotal);
            encabezado.setMinWidth(anchoTotal);
            if (!encabezado.getChildren().isEmpty() && encabezado.getChildren().get(0) instanceof VBox) {
                VBox espacioHora = (VBox) encabezado.getChildren().get(0);
                establecerAncho(espacioHora, ANCHO_HORA);
            }
            for (int i = 1; i < encabezado.getChildren().size(); i++) {
                if (encabezado.getChildren().get(i) instanceof VBox) {
                    VBox dia = (VBox) encabezado.getChildren().get(i);
                    establecerAncho(dia, anchoDia);
                }
            }
        }

        if (contenedorAgenda.getChildren().get(1) instanceof HBox) {
            HBox cuerpo = (HBox) contenedorAgenda.getChildren().get(1);
            cuerpo.setPrefWidth(anchoTotal);
            cuerpo.setMinWidth(anchoTotal);
            if (!cuerpo.getChildren().isEmpty() && cuerpo.getChildren().get(0) instanceof VBox) {
                VBox columnaHoras = (VBox) cuerpo.getChildren().get(0);
                establecerAncho(columnaHoras, ANCHO_HORA);
            }
            for (int i = 1; i < cuerpo.getChildren().size(); i++) {
                if (cuerpo.getChildren().get(i) instanceof AnchorPane) {
                    AnchorPane columnaDia = (AnchorPane) cuerpo.getChildren().get(i);
                    establecerAncho(columnaDia, anchoDia);
                }
            }
        }
        contenedorAgenda.setPrefWidth(anchoTotal);
        contenedorAgenda.setMinWidth(anchoTotal);
        scrollAgenda.setFitToWidth(anchoTotal <= anchoViewport + 1);
    }

    private void establecerAncho(javafx.scene.layout.Region region, double ancho) {
        region.setPrefWidth(ancho);
        region.setMinWidth(ancho);
        region.setMaxWidth(ancho);
    }

    private void actualizarAgenda() {
        indicadorHoraActual = null;
        columnaHoraActual = null;
        contenedorAgenda.getChildren().clear();
        String vista = cmbVista.getSelectionModel().getSelectedItem();
        if ("Día".equals(vista)) {
            actualizarAgendaDia();
        } else if ("Mes".equals(vista)) {
            actualizarAgendaMes();
        } else {
            actualizarAgendaSemana();
        }
        Platform.runLater(() -> {ajustarAnchoAgenda();actualizarIndicadorHoraActual();});
    }

    private void actualizarAgendaSemana() {
        LocalDate inicioSemana = obtenerInicioSemana(fechaActual);
        LocalDate finSemana = inicioSemana.plusDays(6);
        actualizarRangoFecha(inicioSemana, finSemana);
        List<Cita> citas = citaService.obtenerPorRango(inicioSemana.atStartOfDay(), inicioSemana.plusDays(7).atStartOfDay());
        citas = filtrarCitas(citas);
        construirEncabezadoSemana(inicioSemana);
        construirCuerpoSemana(inicioSemana, citas);
    }

    private void actualizarAgendaDia() {
        LocalDate fecha = fechaActual;
        actualizarRangoFechaDia(fecha);
        List<Cita> citas = citaService.obtenerPorRango(fecha.atStartOfDay(), fecha.plusDays(1).atStartOfDay());
        citas = filtrarCitas(citas);
        construirEncabezadoDiaVista(fecha);
        construirCuerpoDia(fecha, citas);
    }

    private void construirEncabezadoDiaVista(LocalDate fecha) {
        HBox encabezado = new HBox();
        encabezado.getStyleClass().add("agenda-week-header");
        VBox espacioHora = new VBox();
        establecerAncho(espacioHora, ANCHO_HORA);
        espacioHora.getStyleClass().add("agenda-time-header");
        encabezado.getChildren().add(espacioHora);
        VBox dia = construirEncabezadoDia(fecha);
        encabezado.getChildren().add(dia);
        contenedorAgenda.getChildren().add(encabezado);
    }

    private void actualizarRangoFechaDia(LocalDate fecha) {
        String nombreDia = fecha.getDayOfWeek().getDisplayName(TextStyle.FULL, LOCALE_ES);
        String nombreMes = fecha.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        String texto = nombreDia.substring(0, 1).toUpperCase() + nombreDia.substring(1) + " " + fecha.getDayOfMonth() + " de " + nombreMes + " de " + fecha.getYear();
        lblRangoFecha.setText(texto);
    }

    private void construirCuerpoDia(LocalDate fecha, List<Cita> citas) {
        HBox cuerpo = new HBox();
        cuerpo.getStyleClass().add("agenda-week-body");
        VBox columnaHoras = construirColumnaHoras();
        cuerpo.getChildren().add(columnaHoras);
        AnchorPane columnaDia = construirColumnaDia(fecha, citas);
        cuerpo.getChildren().add(columnaDia);
        contenedorAgenda.getChildren().add(cuerpo);
    }

    private LocalDate obtenerInicioSemana(LocalDate fecha) {
        return fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
    }

    private void actualizarRangoFecha(LocalDate inicio, LocalDate fin) {
        String textoInicio = inicio.getDayOfMonth() + " " + inicio.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_ES);
        String textoFin = fin.getDayOfMonth() + " " + fin.getMonth().getDisplayName(TextStyle.SHORT, LOCALE_ES) + " " + fin.getYear();
        lblRangoFecha.setText(textoInicio + " - " + textoFin);
    }

    private void construirEncabezadoSemana(LocalDate inicioSemana) {
        HBox encabezado = new HBox();
        encabezado.getStyleClass().add("agenda-week-header");
        VBox espacioHora = new VBox();
        establecerAncho(espacioHora, ANCHO_HORA);
        espacioHora.getStyleClass().add("agenda-time-header");
        encabezado.getChildren().add(espacioHora);

        for (int i = 0; i < NUMERO_DIAS; i++) {

            LocalDate fecha = inicioSemana.plusDays(i);
            VBox dia = construirEncabezadoDia(fecha);
            encabezado.getChildren().add(dia);
        }
        contenedorAgenda.getChildren().add(encabezado);
    }

    private VBox construirEncabezadoDia(LocalDate fecha) {
        VBox dia = new VBox(2);
        establecerAncho(dia, ANCHO_DIA_MINIMO);
        dia.setAlignment(Pos.CENTER);
        dia.getStyleClass().add("agenda-day-header");

        if (fecha.equals(LocalDate.now())) {
            dia.getStyleClass().add("agenda-day-header-today");
        }
        Label nombreDia = new Label(fecha.getDayOfWeek().getDisplayName(TextStyle.SHORT, LOCALE_ES).toUpperCase());
        nombreDia.getStyleClass().add("agenda-day-name");
        Label numeroDia = new Label(String.valueOf(fecha.getDayOfMonth()));
        numeroDia.getStyleClass().add("agenda-day-number");
        dia.getChildren().addAll(nombreDia, numeroDia);
        return dia;
    }

    private void construirCuerpoSemana(LocalDate inicioSemana, List<Cita> citas) {
        HBox cuerpo = new HBox();
        cuerpo.getStyleClass().add("agenda-week-body");
        VBox columnaHoras = construirColumnaHoras();
        cuerpo.getChildren().add(columnaHoras);

        for (int i = 0; i < NUMERO_DIAS; i++) {

            LocalDate fecha = inicioSemana.plusDays(i);
            AnchorPane columnaDia = construirColumnaDia(fecha, citas);
            cuerpo.getChildren().add(columnaDia);
        }
        contenedorAgenda.getChildren().add(cuerpo);
    }

    private VBox construirColumnaHoras() {
        VBox columnaHoras = new VBox();
        establecerAncho(columnaHoras, ANCHO_HORA);
        columnaHoras.getStyleClass().add("agenda-time-column");

        for (int hora = HORA_INICIO; hora < HORA_FIN; hora++) {

            Label etiqueta = new Label(String.format("%02d:00", hora));
            etiqueta.setPrefHeight(ALTURA_POR_HORA);
            etiqueta.setMinHeight(ALTURA_POR_HORA);
            etiqueta.setMaxHeight(ALTURA_POR_HORA);
            etiqueta.setAlignment(Pos.TOP_CENTER);
            etiqueta.getStyleClass().add("agenda-time-label");
            columnaHoras.getChildren().add(etiqueta);
        }
        return columnaHoras;
    }

    private AnchorPane construirColumnaDia(LocalDate fecha, List<Cita> citas) {
        AnchorPane columna = new AnchorPane();
        double alturaTotal = (HORA_FIN - HORA_INICIO) * ALTURA_POR_HORA;
        establecerAncho(columna, ANCHO_DIA_MINIMO);
        columna.setPrefHeight(alturaTotal);
        columna.setMinHeight(alturaTotal);
        columna.setMaxHeight(alturaTotal);
        columna.getStyleClass().add("agenda-day-column");
        construirLineasHorarias(columna, alturaTotal);
        columna.setOnMouseClicked(event -> {if (event.getButton() != MouseButton.PRIMARY) {return;}

            if (event.getTarget() != columna) {
                return;
            }
            LocalTime hora = calcularHoraDesdeY(event.getY());

            if (hora == null) {
                return;
            }

            abrirNuevaCita(fecha, hora);
            event.consume();
        });

        for (Cita cita : citas) {

            if (cita.getInicio() == null || cita.getFin() == null) {
                continue;
            }

            if (!cita.getInicio().toLocalDate().equals(fecha)) {
                continue;
            }
            agregarCita(columna, cita);
        }

        if (fecha.equals(LocalDate.now())) {
            agregarIndicadorHoraActual(columna);
        }
        return columna;
    }

    private LocalTime calcularHoraDesdeY(double y) {
        if (y < 0) {
            return null;
        }
        double minutos = y * 60.0 / ALTURA_POR_HORA;
        int minutosRedondeados = (int) Math.round(minutos / 30.0) * 30;
        minutosRedondeados = Math.max(0, Math.min(11 * 60 + 30, minutosRedondeados));
        int hora = HORA_INICIO + minutosRedondeados / 60;
        int minuto = minutosRedondeados % 60;
        return LocalTime.of(hora, minuto);
    }

    private void agregarIndicadorHoraActual(AnchorPane columna) {
        Pane indicador = new Pane();
        indicador.getStyleClass().add("agenda-current-time-line");
        indicador.setPrefHeight(2);
        indicador.setMinHeight(2);
        indicador.setMaxHeight(2);
        indicador.setMouseTransparent(true);
        AnchorPane.setLeftAnchor(indicador, 0.0);
        AnchorPane.setRightAnchor(indicador, 0.0);
        columna.getChildren().add(indicador);
        indicadorHoraActual = indicador;
        columnaHoraActual = columna;
        actualizarIndicadorHoraActual();
    }

    private void construirLineasHorarias(AnchorPane columna, double alturaTotal) {
        for (int hora = HORA_INICIO; hora <= HORA_FIN; hora++) {
            Pane linea = new Pane();
            linea.getStyleClass().add("agenda-hour-line");
            linea.setMouseTransparent(true);
            double posicion = (hora - HORA_INICIO) * ALTURA_POR_HORA;
            linea.setPrefHeight(1);
            linea.setMinHeight(1);
            linea.setMaxHeight(1);
            AnchorPane.setTopAnchor(linea, posicion);
            AnchorPane.setLeftAnchor(linea, 0.0);
            AnchorPane.setRightAnchor(linea, 0.0);
            columna.getChildren().add(linea);
        }

        for (int hora = HORA_INICIO; hora < HORA_FIN; hora++) {
            Pane lineaMedia = new Pane();
            lineaMedia.getStyleClass().add("agenda-half-hour-line");
            lineaMedia.setMouseTransparent(true);
            double posicion = (hora - HORA_INICIO) * ALTURA_POR_HORA + ALTURA_POR_HORA / 2;
            lineaMedia.setPrefHeight(1);
            lineaMedia.setMinHeight(1);
            lineaMedia.setMaxHeight(1);
            AnchorPane.setTopAnchor(lineaMedia, posicion);
            AnchorPane.setLeftAnchor(lineaMedia, 0.0);
            AnchorPane.setRightAnchor(lineaMedia, 0.0);
            columna.getChildren().add(lineaMedia);
        }
    }

    private void agregarCita(AnchorPane columna, Cita cita) {
        LocalTime horaInicio = cita.getInicio().toLocalTime();
        double minutosDesdeInicio = (horaInicio.getHour() - HORA_INICIO) * 60 + horaInicio.getMinute();
        double duracionMinutos = java.time.Duration.between(cita.getInicio(), cita.getFin()).toMinutes();
        double posicionVertical = minutosDesdeInicio * ALTURA_POR_HORA / 60.0;
        double altura = duracionMinutos * ALTURA_POR_HORA / 60.0;
        altura = Math.max(altura, 34.0);
        VBox bloque = crearBloqueCita(cita);
        AnchorPane.setTopAnchor(bloque, posicionVertical + 2);
        AnchorPane.setLeftAnchor(bloque, 4.0);
        AnchorPane.setRightAnchor(bloque, 4.0);
        double alturaBloque = Math.max(altura - 4, 30);
        bloque.setPrefHeight(alturaBloque);
        bloque.setMinHeight(alturaBloque);
        columna.getChildren().add(bloque);
    }

    private VBox crearBloqueCita(Cita cita) {
        VBox bloque = new VBox(2);
        bloque.getStyleClass().add("agenda-event");
        if (cita.getEstado() != null) {
            switch (cita.getEstado()) {
                case PROGRAMADA:
                    bloque.getStyleClass().add("agenda-event-programada");
                    break;
                case CONFIRMADA:
                    bloque.getStyleClass().add("agenda-event-confirmada");
                    break;
                case ATENDIDA:
                    bloque.getStyleClass().add("agenda-event-atendida");
                    break;
                case CANCELADA:
                    bloque.getStyleClass().add("agenda-event-cancelada");
                    break;
                case NO_ASISTIO:
                    bloque.getStyleClass().add("agenda-event-no-asistio");
                    break;
            }
        }
        Label paciente = new Label(cita.getNombrePaciente());
        paciente.setWrapText(true);
        paciente.getStyleClass().add("agenda-event-patient");
        bloque.getChildren().add(paciente);
        HBox filaHorarioMotivo = new HBox(4);
        filaHorarioMotivo.setAlignment(Pos.CENTER_LEFT);
        filaHorarioMotivo.setFillHeight(true);
        Label horario = new Label(cita.getInicio().format(FORMATO_HORA) + " - " + cita.getFin().format(FORMATO_HORA));
        horario.getStyleClass().add("agenda-event-time");
        filaHorarioMotivo.getChildren().add(horario);

        if (cita.getMotivo() != null && !cita.getMotivo().isBlank()) {
            Label separador = new Label("·");
            separador.getStyleClass().add("agenda-event-time");
            Label motivo = new Label(cita.getMotivo());
            motivo.setWrapText(false);
            motivo.setMaxWidth(Double.MAX_VALUE);
            motivo.getStyleClass().add("agenda-event-reason");
            filaHorarioMotivo.getChildren().addAll(separador, motivo);
        }
        bloque.getChildren().add(filaHorarioMotivo);

        if (cita.getNotas() != null && !cita.getNotas().isBlank()) {
            Label notas = new Label(cita.getNotas());
            notas.setWrapText(true);
            notas.getStyleClass().add("agenda-event-notes");
            bloque.getChildren().add(notas);
        }

        configurarDragAndDrop(bloque, cita);
        bloque.setOnContextMenuRequested(event -> {
            ContextMenu menu = new ContextMenu();
            MenuItem confirmar = new MenuItem("Confirmar");
            MenuItem atendida = new MenuItem("Marcar como atendida");
            MenuItem noAsistio = new MenuItem("Marcar como no asistió");
            MenuItem cancelar = new MenuItem("Cancelar cita");
            MenuItem eliminar = new MenuItem("Eliminar cita");
            confirmar.setOnAction(e -> {
                try {
                    citaService.confirmar(cita.getId());
                    actualizarAgenda();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            atendida.setOnAction(e -> {
                try {
                    citaService.marcarAtendida(cita.getId());
                    actualizarAgenda();

                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            noAsistio.setOnAction(e -> {
                try {
                    citaService.marcarNoAsistio(cita.getId());
                    actualizarAgenda();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });

            cancelar.setOnAction(e -> {
                try {
                    citaService.cancelar(cita.getId());
                    actualizarAgenda();
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            });
            eliminar.setOnAction(e -> {Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar esta cita?\n\n" + cita.getNombrePaciente() + "\n" + cita.getInicio().format(FORMATO_HORA) + " - " + cita.getFin().format(FORMATO_HORA), ButtonType.YES, ButtonType.NO);
                alerta.setTitle("Eliminar cita");
                alerta.setHeaderText("Confirmar eliminación");
                alerta.showAndWait().ifPresent(respuesta -> {
                            if (respuesta == ButtonType.YES) {
                                try {
                                    citaService.eliminar(cita.getId());
                                    actualizarAgenda();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
            });

            Label encabezadoEstado = new Label("Estado de la cita");
            encabezadoEstado.getStyleClass().add("context-menu-section-label");
            CustomMenuItem itemEncabezadoEstado = new CustomMenuItem(encabezadoEstado);
            itemEncabezadoEstado.setHideOnClick(false);
            menu.getItems().add(itemEncabezadoEstado);
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(confirmar);
            menu.getItems().add(atendida);
            menu.getItems().add(noAsistio);
            menu.getItems().add(cancelar);
            menu.getItems().add(new SeparatorMenuItem());
            menu.getItems().add(eliminar);
            menu.show(bloque, event.getScreenX(), event.getScreenY());
            event.consume();
        });
        return bloque;
    }

    private void iniciarResize(VBox bloque, Cita cita, MouseEvent event) {
        redimensionandoCita = true;
        bloqueRedimensionado = bloque;
        citaRedimensionada = cita;
        alturaOriginal = bloque.getPrefHeight();
        alturaInicialResize = event.getSceneY();
        inicioOriginalResize = cita.getInicio();
        finOriginalResize = cita.getFin();
        finPropuestoResize = finOriginalResize;
        resizeValido = true;
        bloque.setOpacity(0.75);
        if (!bloque.getStyleClass().contains("agenda-event-resizing")) {
            bloque.getStyleClass().add("agenda-event-resizing");
        }
    }

    private void actualizarResize(MouseEvent event) {
        if (bloqueRedimensionado == null || citaRedimensionada == null) {
            return;
        }
        double desplazamiento = event.getSceneY() - alturaInicialResize;
        double nuevaAltura = alturaOriginal + desplazamiento;
        int bloques = (int) Math.round(nuevaAltura / (ALTURA_POR_HORA / 2.0));
        bloques = Math.max(1, bloques);
        double alturaFinal = bloques * (ALTURA_POR_HORA / 2.0);
        LocalDateTime nuevoFin = calcularFinDesdeAltura(bloques);
        if (nuevoFin == null) {
            return;
        }
        finPropuestoResize = nuevoFin;
        resizeValido = !existeConflictoDuranteResize(inicioOriginalResize, nuevoFin, citaRedimensionada);
        bloqueRedimensionado.setPrefHeight(alturaFinal);
        bloqueRedimensionado.setMinHeight(alturaFinal);
        bloqueRedimensionado.setMaxHeight(alturaFinal);
        actualizarEstiloResize(resizeValido);
    }

    private LocalDateTime calcularFinDesdeAltura(int bloques) {
        if (inicioOriginalResize == null) {
            return null;
        }
        int minutos = bloques * 30;
        LocalDateTime nuevoFin = inicioOriginalResize.plusMinutes(minutos);
        LocalDateTime limite = inicioOriginalResize.toLocalDate().atTime(HORA_FIN, 0);
        if (nuevoFin.isAfter(limite)) {
            return null;
        }
        return nuevoFin;
    }

    private boolean existeConflictoDuranteResize(LocalDateTime nuevoInicio, LocalDateTime nuevoFin, Cita citaRedimensionada) {
        LocalDateTime inicioDia = nuevoInicio.toLocalDate().atStartOfDay();
        LocalDateTime finDia = nuevoInicio.toLocalDate().plusDays(1).atStartOfDay();
        List<Cita> citas = citaService.obtenerPorRango(inicioDia, finDia);
        for (Cita otra : citas) {
            if (otra == null) {
                continue;
            }
            if (otra.getId() != null && citaRedimensionada.getId() != null && otra.getId().equals(citaRedimensionada.getId())) {
                continue;
            }

            if (otra.getEstado() == EstadoCita.CANCELADA) {
                continue;
            }
            if (otra.getInicio() == null || otra.getFin() == null) {
                continue;
            }
            boolean conflicto = nuevoInicio.isBefore(otra.getFin()) && nuevoFin.isAfter(otra.getInicio());
            if (conflicto) {
                return true;
            }
        }
        return false;
    }

    private void actualizarEstiloResize(boolean valido) {
        if (bloqueRedimensionado == null) {
            return;
        }
        bloqueRedimensionado.getStyleClass().remove("agenda-event-resize-valid");
        bloqueRedimensionado.getStyleClass().remove("agenda-event-resize-conflict");
        bloqueRedimensionado.getStyleClass().add(valido ? "agenda-event-resize-valid" : "agenda-event-resize-conflict");
    }

    private void finalizarResize() {
        if (bloqueRedimensionado == null || citaRedimensionada == null) {
            limpiarEstadoResize();
            return;
        }
        if (!resizeValido || finPropuestoResize == null) {
            actualizarAgenda();
            limpiarEstadoResize();
            return;
        }
        if (finPropuestoResize.equals(finOriginalResize)) {
            actualizarAgenda();
            limpiarEstadoResize();
            return;
        }
        try {
            long nuevaDuracion = java.time.Duration.between(inicioOriginalResize, finPropuestoResize).toMinutes();
            citaService.cambiarDuracion(citaRedimensionada.getId(), nuevaDuracion);
            actualizarAgenda();
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarError("No se pudo cambiar la duración.", ex.getMessage());
            actualizarAgenda();
        } finally {
            limpiarEstadoResize();
        }
    }

    private void limpiarEstadoResize() {
        if (bloqueRedimensionado != null) {
            bloqueRedimensionado.setOpacity(1.0);
            bloqueRedimensionado.setCursor(Cursor.HAND);
            bloqueRedimensionado.getStyleClass().remove("agenda-event-resizing");
            bloqueRedimensionado.getStyleClass().remove("agenda-event-resize-valid");
            bloqueRedimensionado.getStyleClass().remove("agenda-event-resize-conflict");
        }
        redimensionandoCita = false;
        citaRedimensionada = null;
        bloqueRedimensionado = null;
        alturaOriginal = 0;
        alturaInicialResize = 0;
        inicioOriginalResize = null;
        finOriginalResize = null;
        finPropuestoResize = null;
        resizeValido = false;
    }

    private void configurarDragAndDrop(VBox bloque, Cita cita) {
        bloque.setOnMouseMoved(event -> {
            if (redimensionandoCita || arrastrandoCita) {
                return;
            }
            double altura = bloque.getBoundsInLocal().getHeight();
            if (event.getY() >= altura - ZONA_RESIZE) {
                bloque.setCursor(Cursor.S_RESIZE);
            } else {
                bloque.setCursor(Cursor.HAND);
            }
        });
        bloque.setOnMousePressed(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            double altura = bloque.getBoundsInLocal().getHeight();
            if (event.getY() >= altura - ZONA_RESIZE) {
                iniciarResize(bloque, cita, event);
                event.consume();
                return;
            }
            citaArrastrada = bloque;
            citaEnArrastre = cita;
            columnaOriginal = (AnchorPane) bloque.getParent();
            posicionOriginal = obtenerTop(bloque);
            fechaOriginal = cita.getInicio().toLocalDate();
            inicioOriginal = cita.getInicio();
            Point2D puntoLocal = bloque.sceneToLocal(event.getSceneX(), event.getSceneY());
            offsetYArrastre = puntoLocal.getY();
            arrastrandoCita = false;
            arrastreValido = false;
        });
        bloque.setOnMouseDragged(event -> {
            if (redimensionandoCita && citaRedimensionada == cita) {
                actualizarResize(event);
                event.consume();
                return;
            }
            if (citaArrastrada == null || citaEnArrastre == null) {
                return;
            }
            if (event.getButton() != MouseButton.PRIMARY && !event.isPrimaryButtonDown()) {
                return;
            }
            if (!arrastrandoCita) {
                double distanciaX = Math.abs(event.getSceneX() - bloque.localToScene(0, 0).getX());
                double distanciaY = Math.abs(event.getSceneY() - bloque.localToScene(0, 0).getY());
                if (distanciaX < UMBRAL_ARRASTRE && distanciaY < UMBRAL_ARRASTRE) {
                    return;
                }
                iniciarArrastre();
            }
            actualizarPosicionArrastre(event);
        });
        bloque.setOnMouseReleased(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (redimensionandoCita && citaRedimensionada == cita) {
                finalizarResize();
                event.consume();
                return;
            }
            if (!arrastrandoCita) {
                abrirDetalleCita(cita);
                limpiarEstadoArrastre();
                event.consume();
                return;
            }
            finalizarArrastre();
            event.consume();
        });
    }

    private void iniciarArrastre() {
        if (citaArrastrada == null) {
            return;
        }
        arrastrandoCita = true;
        citaArrastrada.setOpacity(0.75);
        if (!citaArrastrada.getStyleClass().contains("agenda-event-dragging")) {
            citaArrastrada.getStyleClass().add("agenda-event-dragging");
        }
    }

    private void actualizarPosicionArrastre(MouseEvent event) {
        if (citaArrastrada == null || citaEnArrastre == null) {
            return;
        }
        AnchorPane columna = obtenerColumnaDesdeEscena(event.getSceneX(), event.getSceneY());
        if (columna == null) {
            return;
        }
        Point2D punto = columna.sceneToLocal(event.getSceneX(), event.getSceneY());
        double posicionY = punto.getY() - offsetYArrastre;
        double duracion = java.time.Duration.between(citaEnArrastre.getInicio(), citaEnArrastre.getFin()).toMinutes();
        duracion = Math.max(duracion, 30);
        LocalTime nuevaHora = calcularHoraDesdePosicion(posicionY, duracion);
        LocalDate nuevaFecha = obtenerFechaDeColumna(columna);
        if (nuevaFecha == null || nuevaHora == null) {
            return;
        }
        LocalDateTime nuevoInicio = LocalDateTime.of(nuevaFecha, nuevaHora);
        LocalDateTime nuevoFin = nuevoInicio.plusMinutes((long) duracion);
        boolean valido = !existeConflictoDuranteArrastre(nuevoInicio, nuevoFin, citaEnArrastre);
        fechaPropuestaArrastre = nuevaFecha;
        horaPropuestaArrastre = nuevaHora;
        arrastreValido = valido;
        columnaDestino = columna;
        double nuevaPosicion = calcularPosicionDesdeHora(nuevaHora);
        if (citaArrastrada.getParent() != columna) {
            if (citaArrastrada.getParent() instanceof AnchorPane) {
                ((AnchorPane) citaArrastrada.getParent()).getChildren().remove(citaArrastrada);
            }
            columna.getChildren().add(citaArrastrada);
            AnchorPane.setLeftAnchor(citaArrastrada, 4.0);
            AnchorPane.setRightAnchor(citaArrastrada, 4.0);
        }
        AnchorPane.setTopAnchor(citaArrastrada, nuevaPosicion + 2);
        actualizarEstiloArrastre(arrastreValido);
    }

    private LocalTime calcularHoraDesdePosicion(double posicionY, double duracionMinutos) {
        double minutos = posicionY * 60.0 / ALTURA_POR_HORA;
        int bloques = (int) Math.round(minutos / 30.0);
        int minutosRedondeados = bloques * 30;
        int duracion = (int) Math.ceil(duracionMinutos / 30.0) * 30;
        int minutosMaximos = (HORA_FIN - HORA_INICIO) * 60 - duracion;
        minutosRedondeados = Math.max(0, Math.min(minutosRedondeados, minutosMaximos));
        return LocalTime.of(HORA_INICIO, 0).plusMinutes(minutosRedondeados);
    }

    private double calcularPosicionDesdeHora(LocalTime hora) {
        double minutosDesdeInicio = (hora.getHour() - HORA_INICIO) * 60.0 + hora.getMinute();
        return minutosDesdeInicio * ALTURA_POR_HORA / 60.0;
    }

    private AnchorPane obtenerColumnaDesdeEscena(double sceneX, double sceneY) {
        if (contenedorAgenda.getChildren().size() < 2) {
            return null;
        }
        if (!(contenedorAgenda.getChildren().get(1) instanceof HBox)) {
            return null;
        }
        HBox cuerpo = (HBox) contenedorAgenda.getChildren().get(1);
        for (int i = 1; i < cuerpo.getChildren().size(); i++) {
            if (!(cuerpo.getChildren().get(i) instanceof AnchorPane)) {
                continue;
            }
            AnchorPane columna = (AnchorPane) cuerpo.getChildren().get(i);
            Bounds bounds = columna.localToScene(columna.getBoundsInLocal());
            if (bounds.contains(sceneX, sceneY)) {
                return columna;
            }
        }
        return null;
    }

    private LocalDate obtenerFechaDeColumna(AnchorPane columna) {
        if (contenedorAgenda.getChildren().size() < 2) {
            return null;
        }
        if (!(contenedorAgenda.getChildren().get(1) instanceof HBox)) {
            return null;
        }
        HBox cuerpo = (HBox) contenedorAgenda.getChildren().get(1);
        String vista = cmbVista.getSelectionModel().getSelectedItem();
        if ("Día".equals(vista)) {
            if (cuerpo.getChildren().size() > 1 && cuerpo.getChildren().get(1) == columna) {
                return fechaActual;
            }
            return null;
        }
        LocalDate inicioSemana = obtenerInicioSemana(fechaActual);
        for (int i = 1; i < cuerpo.getChildren().size(); i++) {
            if (cuerpo.getChildren().get(i) == columna) {
                return inicioSemana.plusDays(i - 1);
            }
        }
        return null;
    }

    private double obtenerTop(VBox bloque) {
        Double top = AnchorPane.getTopAnchor(bloque);
        return top != null ? top : 0.0;
    }

    private boolean existeConflictoDuranteArrastre(LocalDateTime nuevoInicio, LocalDateTime nuevoFin, Cita citaMovida) {
        LocalDateTime inicioDia = nuevoInicio.toLocalDate().atStartOfDay();
        LocalDateTime finDia = nuevoInicio.toLocalDate().plusDays(1).atStartOfDay();
        List<Cita> citas = citaService.obtenerPorRango(inicioDia, finDia);
        for (Cita otra : citas) {
            if (otra == null) {
                continue;
            }
            if (otra.getId() != null && citaMovida.getId() != null && otra.getId().equals(citaMovida.getId())) {
                continue;
            }
            if (otra.getEstado() == mx.dentalcare.domain.cita.EstadoCita.CANCELADA) {
                continue;
            }
            if (otra.getInicio() == null || otra.getFin() == null) {
                continue;
            }
            boolean conflicto = nuevoInicio.isBefore(otra.getFin()) && nuevoFin.isAfter(otra.getInicio());
            if (conflicto) {
                return true;
            }
        }
        return false;
    }

    private void actualizarEstiloArrastre(boolean valido) {
        if (citaArrastrada == null) {
            return;
        }
        citaArrastrada.getStyleClass().remove("agenda-event-drag-valid");
        citaArrastrada.getStyleClass().remove("agenda-event-drag-conflict");
        citaArrastrada.getStyleClass().add(valido ? "agenda-event-drag-valid" : "agenda-event-drag-conflict");
    }

    private void finalizarArrastre() {
        if (citaArrastrada == null || citaEnArrastre == null) {
            limpiarEstadoArrastre();
            return;
        }
        if (!arrastreValido || fechaPropuestaArrastre == null || horaPropuestaArrastre == null) {
            actualizarAgenda();
            limpiarEstadoArrastre();
            return;
        }
        LocalDateTime nuevoInicio = LocalDateTime.of(fechaPropuestaArrastre, horaPropuestaArrastre);
        if (nuevoInicio.equals(inicioOriginal)) {
            actualizarAgenda();
            limpiarEstadoArrastre();
            return;
        }
        try {
            citaService.cambiarInicio(citaEnArrastre.getId(), nuevoInicio);
            actualizarAgenda();
        } catch (Exception ex) {
            ex.printStackTrace();
            mostrarError("No se pudo mover la cita.", ex.getMessage());
            actualizarAgenda();

        } finally {
            limpiarEstadoArrastre();
        }
    }

    private void limpiarEstadoArrastre() {
        if (citaArrastrada != null) {
            citaArrastrada.setOpacity(1.0);
            citaArrastrada.getStyleClass().remove("agenda-event-dragging");
            citaArrastrada.getStyleClass().remove("agenda-event-drag-valid");
            citaArrastrada.getStyleClass().remove("agenda-event-drag-conflict");
        }
        fechaPropuestaArrastre = null;
        horaPropuestaArrastre = null;
        citaArrastrada = null;
        citaEnArrastre = null;
        columnaOriginal = null;
        columnaDestino = null;
        posicionOriginal = 0;
        offsetYArrastre = 0;
        fechaOriginal = null;
        inicioOriginal = null;
        arrastrandoCita = false;
        arrastreValido = false;
    }

    private void mostrarError(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void abrirNuevaCita() {
        abrirNuevaCita(fechaActual, LocalTime.of(8, 0));
    }

    private void abrirNuevaCita(LocalDate fecha, LocalTime hora) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/NuevaCitaDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            URL css = getClass().getResource("/ui/css/dentalcare.css");
            if (css != null) {
                root.getStylesheets().add(css.toExternalForm());
            }
            NuevaCitaController controller = loader.getController();
            controller.prepararNuevaCita(fecha, hora);
            Stage stage = new Stage();
            stage.setTitle("Nueva cita");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 720, 820);
            stage.setScene(scene);
            stage.setMinWidth(720);
            stage.setMinHeight(820);
            stage.setMaxWidth(720);
            stage.setMaxHeight(820);
            stage.setResizable(false);

            stage.showAndWait();
            actualizarAgenda();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirEditarCita(Cita cita) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/NuevaCitaDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            NuevaCitaController controller = loader.getController();
            controller.setCitaEditar(cita);
            Stage stage = new Stage();
            stage.setTitle("Editar cita");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 720, 820);
            stage.setScene(scene);
            stage.setMinWidth(720);
            stage.setMinHeight(820);
            stage.setMaxWidth(720);
            stage.setMaxHeight(820);
            stage.setResizable(false);
            stage.showAndWait();
            actualizarAgenda();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void abrirDetalleCita(Cita cita) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/DetalleCitaDialog.fxml"));
            loader.setControllerFactory(applicationContext::getBean);
            Parent root = loader.load();
            DetalleCitaController controller = loader.getController();
            controller.setCita(cita);
            Stage stage = new Stage();
            stage.setTitle("Detalle de cita");
            stage.initModality(Modality.APPLICATION_MODAL);
            Scene scene = new Scene(root, 520, 560);
            stage.setScene(scene);
            stage.setMinWidth(520);
            stage.setMinHeight(560);
            stage.setMaxWidth(520);
            stage.setMaxHeight(560);
            stage.setResizable(false);
            stage.showAndWait();
            actualizarAgenda();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void configurarBusqueda() {
        txtBuscarPaciente.textProperty().addListener((observable, anterior, nuevo) -> {actualizarAgenda();});
    }

    private List<Cita> filtrarCitas(List<Cita> citas) {
        if (citas == null || citas.isEmpty()) {
            return citas;
        }
        String texto = txtBuscarPaciente.getText();
        if (texto == null || texto.isBlank()) {
            return citas;
        }
        String filtro = texto.trim().toLowerCase(LOCALE_ES);
        return citas.stream().filter(cita -> {
                    if (cita == null) {
                        return false;
                    }
                    String nombre = cita.getNombrePaciente();
                    return nombre != null && nombre.toLowerCase(LOCALE_ES).contains(filtro);}).toList();
    }

    private void actualizarAgendaMes() {
        LocalDate primerDiaMes = fechaActual.withDayOfMonth(1);
        LocalDate ultimoDiaMes = fechaActual.withDayOfMonth(fechaActual.lengthOfMonth());
        actualizarRangoFechaMes(fechaActual);
        List<Cita> citas = citaService.obtenerPorRango(primerDiaMes.atStartOfDay(), ultimoDiaMes.plusDays(1).atStartOfDay());
        citas = filtrarCitas(citas);
        construirCalendarioMes(primerDiaMes, ultimoDiaMes, citas);
    }

    private void actualizarRangoFechaMes(LocalDate fecha) {
        String nombreMes = fecha.getMonth().getDisplayName(TextStyle.FULL, LOCALE_ES);
        String texto = nombreMes.substring(0, 1).toUpperCase() + nombreMes.substring(1) + " " + fecha.getYear();
        lblRangoFecha.setText(texto);
    }

    private void construirCalendarioMes(LocalDate primerDiaMes, LocalDate ultimoDiaMes, List<Cita> citas) {
        VBox calendario = new VBox();
        calendario.getStyleClass().add("agenda-month-calendar");
        calendario.setFillWidth(true);
        construirEncabezadoMes(calendario);
        LocalDate primerDiaCalendario = primerDiaMes.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate ultimoDiaCalendario = ultimoDiaMes.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY));
        LocalDate fecha = primerDiaCalendario;
        while (!fecha.isAfter(ultimoDiaCalendario)) {
            HBox semana = construirSemanaMes(fecha, citas);
            calendario.getChildren().add(semana);
            fecha = fecha.plusDays(7);
        }
        contenedorAgenda.getChildren().add(calendario);
    }

    private void construirEncabezadoMes(VBox calendario) {
        HBox encabezado = new HBox();
        encabezado.getStyleClass().add("agenda-month-header");
        String[] dias = {"LUN", "MAR", "MIÉ", "JUE", "VIE", "SÁB", "DOM"};
        for (String diaTexto : dias) {
            Label dia = new Label(diaTexto);
            dia.setAlignment(Pos.CENTER);
            dia.setMaxWidth(Double.MAX_VALUE);
            HBox.setHgrow(dia, javafx.scene.layout.Priority.ALWAYS);
            dia.getStyleClass().add("agenda-month-day-name");
            encabezado.getChildren().add(dia);
        }
        calendario.getChildren().add(encabezado);
    }

    private HBox construirSemanaMes(LocalDate inicioSemana, List<Cita> citas) {
        HBox semana = new HBox();
        semana.getStyleClass().add("agenda-month-week");
        semana.setFillHeight(true);
        for (int i = 0; i < 7; i++) {
            LocalDate fecha = inicioSemana.plusDays(i);
            VBox celda = construirCeldaMes(fecha, citas);
            HBox.setHgrow(celda, javafx.scene.layout.Priority.ALWAYS);
            celda.setMinWidth(0);
            celda.setPrefWidth(0);
            celda.setMaxWidth(Double.MAX_VALUE);
            semana.getChildren().add(celda);
        }
        return semana;
    }

    private VBox construirCeldaMes(LocalDate fecha, List<Cita> citas) {
        VBox celda = new VBox(5);
        celda.setMinWidth(0);
        celda.setMaxWidth(Double.MAX_VALUE);
        celda.getStyleClass().add("agenda-month-cell");
        celda.setPrefHeight(125);
        celda.setMinHeight(125);
        celda.setMaxHeight(125);
        celda.setClip(new javafx.scene.shape.Rectangle(0, 0, 1000, 125));
        if (!fecha.getMonth().equals(fechaActual.getMonth())) {
            celda.getStyleClass().add("agenda-month-cell-other");
        }
        if (fecha.equals(LocalDate.now())) {
            celda.getStyleClass().add("agenda-month-cell-today");
        }
        HBox encabezadoDia = new HBox();
        encabezadoDia.setAlignment(Pos.CENTER_RIGHT);
        Label numero = new Label(String.valueOf(fecha.getDayOfMonth()));
        numero.getStyleClass().add("agenda-month-day-number");
        encabezadoDia.getChildren().add(numero);
        celda.getChildren().add(encabezadoDia);
        List<Cita> citasDelDia = citas.stream().filter(cita -> cita != null && cita.getInicio() != null && cita.getFin() != null && cita.getInicio().toLocalDate().equals(fecha)).sorted((a, b) -> a.getInicio().compareTo(b.getInicio())).toList();
        int citasVisibles = Math.min(citasDelDia.size(), MAX_CITAS_VISIBLES_MES);
        for (int i = 0; i < citasVisibles; i++) {
            agregarCitaMes(celda, citasDelDia.get(i));
        }
        int citasRestantes = citasDelDia.size() - citasVisibles;
        if (citasRestantes > 0) {
            Label masCitas = new Label("+ " + citasRestantes + (citasRestantes == 1 ? " más" : " más"));
            masCitas.setMaxWidth(Double.MAX_VALUE);
            masCitas.getStyleClass().add("agenda-month-more");
            masCitas.setOnMouseClicked(event -> {
                if (event.getButton() != MouseButton.PRIMARY) {
                    return;
                }
                abrirVistaDia(fecha);
                event.consume();
            });
            celda.getChildren().add(masCitas);
        }
        celda.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            if (event.getTarget() == celda || event.getTarget() == encabezadoDia) {
                abrirNuevaCita(fecha, LocalTime.of(8, 0));
                event.consume();
            }
        });
        return celda;
    }

    private void agregarCitaMes(VBox celda, Cita cita) {
        Label evento = new Label();
        evento.setMinWidth(0);
        evento.setPrefWidth(0);
        evento.setMaxWidth(Double.MAX_VALUE);
        evento.setMinHeight(22);
        evento.setPrefHeight(22);
        evento.setWrapText(false);
        evento.setText(cita.getInicio().format(FORMATO_HORA) + "  " + cita.getNombrePaciente());
        evento.getStyleClass().add("agenda-month-event");
        if (cita.getEstado() != null) {
            switch (cita.getEstado()) {
                case PROGRAMADA:
                    evento.getStyleClass().add("agenda-event-programada");
                    break;
                case CONFIRMADA:
                    evento.getStyleClass().add("agenda-event-confirmada");
                    break;
                case ATENDIDA:
                    evento.getStyleClass().add("agenda-event-atendida");
                    break;
                case CANCELADA:
                    evento.getStyleClass().add("agenda-event-cancelada");
                    break;
                case NO_ASISTIO:
                    evento.getStyleClass().add("agenda-event-no-asistio");
                    break;
            }
        }
        evento.setOnMouseClicked(event -> {
            if (event.getButton() != MouseButton.PRIMARY) {
                return;
            }
            abrirDetalleCita(cita);
            event.consume();
        });
        configurarMenuContextualMes(evento, cita);
        celda.getChildren().add(evento);
    }
    private void abrirVistaDia(LocalDate fecha) {
        if (fecha == null) {
            return;
        }
        fechaActual = fecha;
        cmbVista.getSelectionModel().select("Día");
        actualizarAgenda();
    }
    private void configurarMenuContextualMes(Label evento, Cita cita) {
        evento.setOnContextMenuRequested(event -> {
            ContextMenu menu = new ContextMenu();
            MenuItem confirmar = new MenuItem("Confirmar");
            MenuItem atendida = new MenuItem("Marcar como atendida");
            MenuItem noAsistio = new MenuItem("Marcar como no asistió");
            MenuItem cancelar = new MenuItem("Cancelar cita");
            MenuItem eliminar = new MenuItem("Eliminar cita");
            confirmar.setOnAction(e -> {try {citaService.confirmar(cita.getId());actualizarAgenda();} catch (Exception ex) {ex.printStackTrace();}});
            atendida.setOnAction(e -> {try {citaService.marcarAtendida(cita.getId());actualizarAgenda();} catch (Exception ex) {ex.printStackTrace();}});
            noAsistio.setOnAction(e -> {try {citaService.marcarNoAsistio(cita.getId());actualizarAgenda();} catch (Exception ex) {ex.printStackTrace();}});
            cancelar.setOnAction(e -> {try {citaService.cancelar(cita.getId());actualizarAgenda();} catch (Exception ex) {ex.printStackTrace();}});
            eliminar.setOnAction(e -> {Alert alerta = new Alert(Alert.AlertType.CONFIRMATION, "¿Deseas eliminar esta cita?\n\n" + cita.getNombrePaciente() + "\n" + cita.getInicio().format(FORMATO_HORA) + " - " + cita.getFin().format(FORMATO_HORA), ButtonType.YES, ButtonType.NO);
                alerta.setTitle("Eliminar cita");
                alerta.setHeaderText(
                        "Confirmar eliminación"
                );

                alerta.showAndWait().ifPresent(respuesta -> {
                            if (respuesta == ButtonType.YES) {
                                try {
                                    citaService.eliminar(cita.getId());
                                    actualizarAgenda();
                                } catch (Exception ex) {
                                    ex.printStackTrace();
                                }
                            }
                        });
            });
            menu.getItems().addAll(confirmar, atendida, noAsistio, cancelar, new SeparatorMenuItem(), eliminar);
            menu.show(evento, event.getScreenX(), event.getScreenY());
            event.consume();
        });
    }
}