package mx.dentalcare.ui.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mx.dentalcare.security.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class MainController {
    @FXML private StackPane contentArea;
    @FXML private Button inicioButton, pacientesButton, citasButton, tratamientosButton, historialButton, finanzasButton, configuracionButton;
    @FXML private Label userNameLabel;
    @FXML private Button userMenuButton;
    private final ApplicationContext context;
    private final SecuritySession securitySession;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public MainController(ApplicationContext context, SecuritySession securitySession, UserService userService, AuthenticationService authenticationService) { this.context = context; this.securitySession = securitySession; this.userService = userService; this.authenticationService = authenticationService; }
    @FXML public void initialize() { configurarPrivilegios(); configurarUsuario(); mostrarInicio(); }
    @FXML private void mostrarInicio() { cargarVista("/ui/fxml/DashboardView.fxml"); }
    @FXML private void mostrarPacientes() { cargarVista("/ui/fxml/PacientesView.fxml"); }
    @FXML private void mostrarCitas() { cargarVista("/ui/fxml/AgendaView.fxml"); }
    @FXML private void mostrarTratamientos() { cargarVista("/ui/fxml/TratamientosView.fxml"); }
    @FXML private void mostrarHistorial() { cargarVista("/ui/fxml/HistorialView.fxml"); }
    @FXML private void mostrarFinanzas() { cargarVista("/ui/fxml/FinanzasView.fxml"); }
    @FXML private void mostrarConfiguracion() { cargarVista("/ui/fxml/ConfiguracionView.fxml"); }

    @FXML private void mostrarMenuUsuario() {
        AuthenticatedUser user = securitySession.requireCurrentUser(); ContextMenu menu = new ContextMenu();
        MenuItem perfil = new MenuItem("Mi perfil"); perfil.setOnAction(event -> mostrarPerfil());
        MenuItem cambiarPassword = new MenuItem("Cambiar contraseña"); cambiarPassword.setOnAction(event -> cambiarPassword());
        menu.getItems().addAll(perfil, cambiarPassword, new SeparatorMenuItem());
        if (user.isAdmin()) {
            MenuItem usuarios = new MenuItem("Usuarios"); usuarios.setOnAction(event -> mostrarUsuarios());
            MenuItem roles = new MenuItem("Roles y permisos"); roles.setOnAction(event -> mostrarRolesPermisos());
            MenuItem auditoria = new MenuItem("Auditoría"); auditoria.setOnAction(event -> mostrarAuditoria());
            menu.getItems().addAll(usuarios, roles, auditoria, new SeparatorMenuItem());
        }
        MenuItem cerrarSesion = new MenuItem("Cerrar sesión"); cerrarSesion.setOnAction(event -> cerrarSesion()); menu.getItems().add(cerrarSesion); menu.show(userMenuButton, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    private void configurarUsuario() { AuthenticatedUser user = securitySession.requireCurrentUser(); userNameLabel.setText(user.getDisplayName()); userMenuButton.setText("⋮"); userMenuButton.setTooltip(new Tooltip(user.getRole().getDisplayName())); }
    private void configurarPrivilegios() { AuthenticatedUser user = securitySession.requireCurrentUser(); configurarBoton(pacientesButton, user.hasPermission(UserPermission.VER_PACIENTES)); configurarBoton(citasButton, user.hasPermission(UserPermission.GESTIONAR_CITAS)); configurarBoton(tratamientosButton, user.hasPermission(UserPermission.VER_TRATAMIENTOS)); configurarBoton(historialButton, user.hasPermission(UserPermission.VER_HISTORIAL)); configurarBoton(finanzasButton, user.hasPermission(UserPermission.VER_FINANZAS)); configurarBoton(configuracionButton, user.hasPermission(UserPermission.VER_CONFIGURACION)); }
    private void configurarBoton(Button button, boolean visible) { button.setManaged(visible); button.setVisible(visible); }

    private void mostrarPerfil() {
        AuthenticatedUser user = securitySession.requireCurrentUser(); Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Mi perfil"); dialog.setHeaderText("Información de tu cuenta");
        VBox content = new VBox(12); content.setPadding(new Insets(8, 4, 8, 4));
        Label nombre = new Label("Nombre visible:  " + user.getDisplayName()); Label usuario = new Label("Usuario:  " + user.getUsername()); Label rol = new Label("Rol:  " + user.getRole().getDisplayName());
        nombre.setWrapText(true); usuario.setWrapText(true); rol.setWrapText(true); content.getChildren().addAll(nombre, usuario, rol);
        DialogPane pane = dialog.getDialogPane(); pane.setContent(content); pane.setPrefWidth(520); pane.setMinWidth(520); pane.setPrefHeight(260); pane.getButtonTypes().add(ButtonType.OK);
        Button cerrar = (Button) pane.lookupButton(ButtonType.OK); if (cerrar != null) cerrar.setText("Cerrar"); estilizarBotonesDialogo(pane); dialog.showAndWait();
    }

    private void cambiarPassword() {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Cambiar contraseña"); dialog.setHeaderText("Actualiza la contraseña de tu usuario");
        PasswordField actual = new PasswordField(); actual.setPromptText("Contraseña actual"); actual.setPrefWidth(300); PasswordField nueva = new PasswordField(); nueva.setPromptText("Nueva contraseña"); nueva.setPrefWidth(300); PasswordField confirmar = new PasswordField(); confirmar.setPromptText("Confirmar nueva contraseña"); confirmar.setPrefWidth(300);
        GridPane grid = new GridPane(); grid.setHgap(14); grid.setVgap(14); grid.setPadding(new Insets(8)); grid.add(new Label("Actual:"), 0, 0); grid.add(actual, 1, 0); grid.add(new Label("Nueva:"), 0, 1); grid.add(nueva, 1, 1); grid.add(new Label("Confirmar:"), 0, 2); grid.add(confirmar, 1, 2);
        DialogPane pane = dialog.getDialogPane(); pane.setContent(grid); pane.setPrefWidth(560); pane.setMinWidth(560); pane.setPrefHeight(330); pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        Button guardar = (Button) pane.lookupButton(ButtonType.OK); if (guardar != null) guardar.setText("Guardar"); Button cancelar = (Button) pane.lookupButton(ButtonType.CANCEL); if (cancelar != null) cancelar.setText("Cancelar"); estilizarBotonesDialogo(pane);
        Optional<ButtonType> result = dialog.showAndWait(); if (result.isEmpty() || result.get() != ButtonType.OK) return; if (!nueva.getText().equals(confirmar.getText())) { mostrarError("Las nuevas contraseñas no coinciden."); return; }
        try { authenticationService.changePassword(actual.getText(), nueva.getText()); mostrarInformacion("Contraseña actualizada", "Tu contraseña fue cambiada correctamente."); } catch (Exception e) { mostrarError(e.getMessage() == null ? "No fue posible cambiar la contraseña." : e.getMessage()); }
    }

    private void mostrarUsuarios() {
        Stage stage = new Stage(); stage.setTitle("DentalCare | Usuarios"); stage.setMinWidth(980); stage.setMinHeight(650);
        TableView<UserService.StoredUser> table = new TableView<>(); table.getStyleClass().add("users-table"); table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        TableColumn<UserService.StoredUser, String> usuario = new TableColumn<>("Usuario"); usuario.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username)); TableColumn<UserService.StoredUser, String> nombre = new TableColumn<>("Nombre"); nombre.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayName)); TableColumn<UserService.StoredUser, String> rol = new TableColumn<>("Rol"); rol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().role.getDisplayName())); TableColumn<UserService.StoredUser, String> estado = new TableColumn<>("Estado"); estado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().active ? "Activo" : "Inactivo")); table.getColumns().addAll(usuario, nombre, rol, estado);
        usuario.setMaxWidth(Double.MAX_VALUE); nombre.setMaxWidth(Double.MAX_VALUE); rol.setMaxWidth(Double.MAX_VALUE); estado.setMaxWidth(Double.MAX_VALUE); usuario.prefWidthProperty().bind(table.widthProperty().multiply(0.25)); nombre.prefWidthProperty().bind(table.widthProperty().multiply(0.35)); rol.prefWidthProperty().bind(table.widthProperty().multiply(0.20)); estado.prefWidthProperty().bind(table.widthProperty().multiply(0.20)); table.getItems().setAll(userService.listUsers());
        TextField nuevoUsuario = new TextField(); nuevoUsuario.setPromptText("usuario"); nuevoUsuario.getStyleClass().add("users-field"); TextField nuevoNombre = new TextField(); nuevoNombre.setPromptText("nombre visible"); nuevoNombre.getStyleClass().add("users-field"); PasswordField nuevaPassword = new PasswordField(); nuevaPassword.setPromptText("mínimo 8 caracteres"); nuevaPassword.getStyleClass().add("users-field"); ComboBox<UserRole> nuevoRol = new ComboBox<>(); nuevoRol.getItems().addAll(UserRole.ADMINISTRADOR, UserRole.USUARIO); nuevoRol.setValue(UserRole.USUARIO); nuevoRol.getStyleClass().add("users-combo"); nuevoRol.setMaxWidth(Double.MAX_VALUE);
        Button crear = new Button("Crear usuario"); crear.getStyleClass().addAll("users-action-button", "users-primary-button"); Button editar = new Button("Editar nombre"); editar.getStyleClass().addAll("users-action-button", "users-secondary-button"); Button cambiarEstado = new Button("Activar / desactivar"); cambiarEstado.getStyleClass().addAll("users-action-button", "users-secondary-button"); crear.setMaxWidth(Double.MAX_VALUE); editar.setMaxWidth(Double.MAX_VALUE); cambiarEstado.setMaxWidth(Double.MAX_VALUE);
        crear.setOnAction(event -> { try { String username = nuevoUsuario.getText().trim(); String displayName = nuevoNombre.getText().trim(); String password = nuevaPassword.getText(); UserRole role = nuevoRol.getValue(); if (username.isBlank()) { mostrarError("El usuario es obligatorio."); return; } if (displayName.isBlank()) { mostrarError("El nombre visible es obligatorio."); return; } if (password == null || password.length() < 8) { mostrarError("La contraseña debe tener al menos 8 caracteres."); return; } if (role == null) { mostrarError("Selecciona un rol."); return; } String confirmacion = "Usuario: " + username + "\nNombre visible: " + displayName + "\nRol: " + role.getDisplayName() + "\n\nVerifica que el usuario esté escrito correctamente.\nEl nombre de usuario no podrá modificarse posteriormente.\n\n¿Deseas crear esta cuenta?"; if (!confirmarAccion("Confirmar creación", "Verifica los datos del usuario", confirmacion)) return; userService.createUser(username, displayName, password, role); table.getItems().setAll(userService.listUsers()); nuevoUsuario.clear(); nuevoNombre.clear(); nuevaPassword.clear(); nuevoRol.setValue(UserRole.USUARIO); mostrarInformacion("Usuario creado", "El usuario ya puede iniciar sesión."); } catch (Exception e) { mostrarError(e.getMessage() == null ? "No fue posible crear el usuario." : e.getMessage()); } });
        editar.setOnAction(event -> { UserService.StoredUser selected = table.getSelectionModel().getSelectedItem(); if (selected == null) { mostrarError("Selecciona un usuario primero."); return; } editarNombreUsuario(selected, table); });
        cambiarEstado.setOnAction(event -> { UserService.StoredUser selected = table.getSelectionModel().getSelectedItem(); if (selected == null) { mostrarError("Selecciona un usuario primero."); return; } try { userService.setUserActive(selected.username, !selected.active); table.getItems().setAll(userService.listUsers()); } catch (Exception e) { mostrarError(e.getMessage() == null ? "No fue posible cambiar el estado." : e.getMessage()); } });
        Label formTitle = new Label("Crear nuevo usuario"); formTitle.getStyleClass().add("users-form-title"); Label lUsuario = new Label("Usuario"); lUsuario.getStyleClass().add("users-form-label"); Label lNombre = new Label("Nombre visible"); lNombre.getStyleClass().add("users-form-label"); Label lPassword = new Label("Contraseña"); lPassword.getStyleClass().add("users-form-label"); Label lRol = new Label("Rol"); lRol.getStyleClass().add("users-form-label");
        VBox usuarioBox = new VBox(6, lUsuario, nuevoUsuario); VBox nombreBox = new VBox(6, lNombre, nuevoNombre); VBox passwordBox = new VBox(6, lPassword, nuevaPassword); VBox rolBox = new VBox(6, lRol, nuevoRol); usuarioBox.setMaxWidth(Double.MAX_VALUE); nombreBox.setMaxWidth(Double.MAX_VALUE); passwordBox.setMaxWidth(Double.MAX_VALUE); rolBox.setMaxWidth(Double.MAX_VALUE); VBox form = new VBox(14, formTitle, usuarioBox, nombreBox, passwordBox, rolBox, crear); form.getStyleClass().add("users-form-card");
        Label title = new Label("Usuarios"); title.getStyleClass().add("users-title"); Label subtitle = new Label("Administra las cuentas y privilegios de acceso a DentalCare."); subtitle.getStyleClass().add("users-subtitle"); VBox header = new VBox(4, title, subtitle); header.getStyleClass().add("users-header"); VBox tablePanel = new VBox(table); tablePanel.getStyleClass().add("users-table-panel"); VBox.setVgrow(table, Priority.ALWAYS); VBox actions = new VBox(10, editar, cambiarEstado); actions.getStyleClass().add("users-selection-actions"); Label actionsTitle = new Label("Usuario seleccionado"); actionsTitle.getStyleClass().add("users-form-title"); VBox rightPanel = new VBox(14, tablePanel, actionsTitle, actions); rightPanel.getStyleClass().add("users-right-panel"); VBox.setVgrow(tablePanel, Priority.ALWAYS); HBox mainContent = new HBox(18, form, rightPanel); mainContent.getStyleClass().add("users-main-content"); HBox.setHgrow(rightPanel, Priority.ALWAYS); form.setPrefWidth(300); form.setMinWidth(280); form.setMaxWidth(340); VBox root = new VBox(0, header, mainContent); root.getStyleClass().add("users-root"); VBox.setVgrow(mainContent, Priority.ALWAYS);
        Scene scene = new Scene(root, 1100, 700); String css = getClass().getResource("/ui/css/users.css").toExternalForm(); scene.getStylesheets().add(css); stage.setScene(scene); stage.setResizable(true); stage.show(); stage.centerOnScreen();
    }

    private void editarNombreUsuario(UserService.StoredUser selected, TableView<UserService.StoredUser> table) {
        Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle("Editar usuario"); dialog.setHeaderText("Editar nombre visible"); TextField nombre = new TextField(selected.displayName); nombre.setPrefWidth(360); nombre.getStyleClass().add("users-field"); Label usuario = new Label("Usuario: " + selected.username); usuario.getStyleClass().add("users-edit-username"); Label nota = new Label("El usuario no puede modificarse."); nota.getStyleClass().add("users-edit-note"); VBox content = new VBox(8, usuario, new Label("Nombre visible"), nombre, nota); content.setPadding(new Insets(6, 8, 4, 8));
        DialogPane pane = dialog.getDialogPane(); pane.setContent(content); pane.setPrefWidth(500); pane.setMinWidth(500); pane.setPrefHeight(330); pane.setMinHeight(330); pane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL); Button guardar = (Button) pane.lookupButton(ButtonType.OK); if (guardar != null) guardar.setText("Guardar"); Button cancelar = (Button) pane.lookupButton(ButtonType.CANCEL); if (cancelar != null) cancelar.setText("Cancelar"); estilizarBotonesDialogo(pane);
        Optional<ButtonType> result = dialog.showAndWait(); if (result.isEmpty() || result.get() != ButtonType.OK) return; try { userService.updateDisplayName(selected.username, nombre.getText()); table.getItems().setAll(userService.listUsers()); configurarUsuario(); mostrarInformacion("Usuario actualizado", "El nombre visible fue actualizado correctamente."); } catch (Exception e) { mostrarError(e.getMessage() == null ? "No fue posible actualizar el usuario." : e.getMessage()); }
    }

    private boolean confirmarAccion(String titulo, String encabezado, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION); alert.setTitle(titulo); alert.setHeaderText(encabezado);
        Label label = new Label(mensaje); label.setWrapText(true); label.setMinWidth(520); label.setPrefWidth(520); label.setMaxWidth(520); label.setMinHeight(Region.USE_PREF_SIZE); label.setPrefHeight(110); label.setStyle("-fx-font-size: 14px; -fx-text-fill: #374151;");
        VBox content = new VBox(label); content.setPadding(new Insets(10, 14, 6, 14)); content.setMinWidth(550); content.setPrefWidth(550);
        alert.getDialogPane().setContent(content); alert.getDialogPane().setPrefWidth(640); alert.getDialogPane().setMinWidth(640); alert.getDialogPane().setPrefHeight(300); alert.getDialogPane().setMinHeight(300);
        Button ok = (Button) alert.getDialogPane().lookupButton(ButtonType.OK); if (ok != null) { ok.setText("Crear usuario"); ok.setMinWidth(140); ok.setPrefWidth(140); }
        Button cancel = (Button) alert.getDialogPane().lookupButton(ButtonType.CANCEL); if (cancel != null) { cancel.setText("Cancelar"); cancel.setMinWidth(110); cancel.setPrefWidth(110); }
        Optional<ButtonType> result = alert.showAndWait(); return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void mostrarRolesPermisos() { StringBuilder texto = new StringBuilder(); for (UserRole role : UserRole.values()) { texto.append(role.getDisplayName()).append("\n"); for (UserPermission permission : AuthenticatedUser.permissionsFor(role)) texto.append("  • ").append(nombrePermiso(permission)).append("\n"); texto.append("\n"); } mostrarDialogoTexto("Roles y permisos", "Permisos actuales de DentalCare", texto.toString(), 620, 560); }
    private void mostrarAuditoria() { mostrarDialogoTexto("Auditoría", "Auditoría de seguridad", "La auditoría detallada se habilitará como siguiente capa del sistema.\n\nLos roles y privilegios ya están activos.", 600, 300); }
    private void mostrarDialogoTexto(String titulo, String encabezado, String texto, double ancho, double alto) { Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(titulo); if (encabezado != null) dialog.setHeaderText(encabezado); Label label = new Label(texto); label.setWrapText(true); label.setMaxWidth(ancho - 70); label.setPadding(new Insets(8)); DialogPane pane = dialog.getDialogPane(); pane.setContent(label); pane.setPrefWidth(ancho); pane.setMinWidth(ancho); pane.setPrefHeight(alto); pane.getButtonTypes().add(ButtonType.OK); estilizarBotonesDialogo(pane); dialog.showAndWait(); }

    private void estilizarBotonesDialogo(DialogPane pane) {
        for (ButtonType tipo : pane.getButtonTypes()) {
            Button button = (Button) pane.lookupButton(tipo);
            if (button != null) {
                if (tipo == ButtonType.OK) button.setText("Guardar"); else if (tipo == ButtonType.CANCEL) button.setText("Cancelar"); else button.setText("Cerrar");
                button.setMinWidth(110); button.setPrefWidth(Region.USE_COMPUTED_SIZE); button.setMinHeight(38); button.setPadding(new Insets(8, 18, 8, 18));
            }
        }
    }

    private String nombrePermiso(UserPermission permission) { return switch (permission) { case VER_INICIO -> "Inicio"; case VER_PACIENTES -> "Pacientes"; case GESTIONAR_CITAS -> "Agenda y citas"; case VER_TRATAMIENTOS -> "Tratamientos"; case VER_HISTORIAL -> "Historial"; case VER_FINANZAS -> "Finanzas"; case VER_CONFIGURACION -> "Configuración"; case GESTIONAR_USUARIOS -> "Gestión de usuarios"; case VER_ROLES -> "Roles y permisos"; case VER_AUDITORIA -> "Auditoría"; }; }
    private void cerrarSesion() { try { authenticationService.logout(); Stage applicationStage = (Stage) contentArea.getScene().getWindow(); applicationStage.close(); FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/LoginView.fxml")); loader.setControllerFactory(context::getBean); Parent root = loader.load(); Stage loginStage = new Stage(); loginStage.initStyle(StageStyle.UNDECORATED); loginStage.setTitle("DentalCare | Iniciar sesión"); loginStage.setScene(new Scene(root, 900, 540)); loginStage.setResizable(false); loginStage.show(); loginStage.centerOnScreen(); } catch (Exception e) { mostrarError("No fue posible cerrar la sesión correctamente."); } }
    private void cargarVista(String ruta) { try { FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta)); loader.setControllerFactory(context::getBean); Parent view = loader.load(); contentArea.getChildren().setAll(view); } catch (Exception e) { throw new RuntimeException("No fue posible cargar la vista: " + ruta, e); } }
    private void mostrarInformacion(String titulo, String mensaje) { Dialog<ButtonType> dialog = new Dialog<>(); dialog.setTitle(titulo); Label label = new Label(mensaje); label.setWrapText(true); label.setAlignment(Pos.CENTER); label.setTextAlignment(javafx.scene.text.TextAlignment.CENTER); label.setStyle("-fx-font-size: 17px; -fx-font-weight: bold; -fx-text-fill: #1f2937;"); label.setMaxWidth(330); label.setPrefWidth(330); label.setMinHeight(55); VBox content = new VBox(label); content.setAlignment(Pos.CENTER); content.setPadding(new Insets(18, 24, 12, 24)); DialogPane pane = dialog.getDialogPane(); pane.setContent(content); pane.setPrefWidth(390); pane.setMinWidth(390); pane.setPrefHeight(155); pane.setMinHeight(155); pane.getButtonTypes().add(ButtonType.OK); estilizarBotonesDialogo(pane); dialog.showAndWait(); }
    private void mostrarError(String mensaje) { mostrarDialogoTexto("DentalCare", null, mensaje, 520, 250); }
}
