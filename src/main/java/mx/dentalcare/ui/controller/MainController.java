package mx.dentalcare.ui.controller;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import mx.dentalcare.security.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button inicioButton;
    @FXML private Button pacientesButton;
    @FXML private Button citasButton;
    @FXML private Button tratamientosButton;
    @FXML private Button historialButton;
    @FXML private Button finanzasButton;
    @FXML private Button configuracionButton;
    @FXML private Label userNameLabel;
    @FXML private Button userMenuButton;

    private final ApplicationContext context;
    private final SecuritySession securitySession;
    private final UserService userService;
    private final AuthenticationService authenticationService;

    public MainController(
            ApplicationContext context,
            SecuritySession securitySession,
            UserService userService,
            AuthenticationService authenticationService
    ) {
        this.context = context;
        this.securitySession = securitySession;
        this.userService = userService;
        this.authenticationService = authenticationService;
    }

    @FXML
    public void initialize() {
        configurarPrivilegios();
        configurarUsuario();
        mostrarInicio();
    }

    @FXML private void mostrarInicio() { cargarVista("/ui/fxml/DashboardView.fxml"); }
    @FXML private void mostrarPacientes() { cargarVista("/ui/fxml/PacientesView.fxml"); }
    @FXML private void mostrarCitas() { cargarVista("/ui/fxml/AgendaView.fxml"); }
    @FXML private void mostrarTratamientos() { cargarVista("/ui/fxml/TratamientosView.fxml"); }
    @FXML private void mostrarHistorial() { cargarVista("/ui/fxml/HistorialView.fxml"); }
    @FXML private void mostrarFinanzas() { cargarVista("/ui/fxml/FinanzasView.fxml"); }
    @FXML private void mostrarConfiguracion() { cargarVista("/ui/fxml/ConfiguracionView.fxml"); }

    @FXML
    private void mostrarMenuUsuario() {
        AuthenticatedUser user = securitySession.requireCurrentUser();
        ContextMenu menu = new ContextMenu();

        MenuItem perfil = new MenuItem("Mi perfil");
        perfil.setOnAction(event -> mostrarPerfil());

        MenuItem cambiarPassword = new MenuItem("Cambiar contraseña");
        cambiarPassword.setOnAction(event -> cambiarPassword());

        menu.getItems().addAll(perfil, cambiarPassword, new SeparatorMenuItem());

        if (user.isAdmin()) {
            MenuItem usuarios = new MenuItem("Usuarios");
            usuarios.setOnAction(event -> mostrarUsuarios());
            MenuItem roles = new MenuItem("Roles y permisos");
            roles.setOnAction(event -> mostrarRolesPermisos());
            MenuItem auditoria = new MenuItem("Auditoría");
            auditoria.setOnAction(event -> mostrarAuditoria());
            menu.getItems().addAll(usuarios, roles, auditoria, new SeparatorMenuItem());
        }

        MenuItem cerrarSesion = new MenuItem("Cerrar sesión");
        cerrarSesion.setOnAction(event -> cerrarSesion());
        menu.getItems().add(cerrarSesion);

        menu.show(userMenuButton, javafx.geometry.Side.BOTTOM, 0, 6);
    }

    private void configurarUsuario() {
        AuthenticatedUser user = securitySession.requireCurrentUser();
        userNameLabel.setText(user.getDisplayName());
        userMenuButton.setText("⋮");
        userMenuButton.setTooltip(new Tooltip(user.getRole().getDisplayName()));
    }

    private void configurarPrivilegios() {
        AuthenticatedUser user = securitySession.requireCurrentUser();
        pacientesButton.setManaged(user.hasPermission(UserPermission.VER_PACIENTES));
        pacientesButton.setVisible(user.hasPermission(UserPermission.VER_PACIENTES));
        citasButton.setManaged(user.hasPermission(UserPermission.GESTIONAR_CITAS));
        citasButton.setVisible(user.hasPermission(UserPermission.GESTIONAR_CITAS));
        tratamientosButton.setManaged(user.hasPermission(UserPermission.VER_TRATAMIENTOS));
        tratamientosButton.setVisible(user.hasPermission(UserPermission.VER_TRATAMIENTOS));
        historialButton.setManaged(user.hasPermission(UserPermission.VER_HISTORIAL));
        historialButton.setVisible(user.hasPermission(UserPermission.VER_HISTORIAL));
        finanzasButton.setManaged(user.hasPermission(UserPermission.VER_FINANZAS));
        finanzasButton.setVisible(user.hasPermission(UserPermission.VER_FINANZAS));
        configuracionButton.setManaged(user.hasPermission(UserPermission.VER_CONFIGURACION));
        configuracionButton.setVisible(user.hasPermission(UserPermission.VER_CONFIGURACION));
    }

    private void mostrarPerfil() {
        AuthenticatedUser user = securitySession.requireCurrentUser();
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Mi perfil");
        alert.setHeaderText(user.getDisplayName());
        alert.setContentText("Usuario: " + user.getUsername() + "\nRol: " + user.getRole().getDisplayName());
        alert.showAndWait();
    }

    private void cambiarPassword() {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Cambiar contraseña");
        dialog.setHeaderText("Actualiza la contraseña de tu usuario");

        PasswordField actual = new PasswordField();
        actual.setPromptText("Contraseña actual");
        PasswordField nueva = new PasswordField();
        nueva.setPromptText("Nueva contraseña");
        PasswordField confirmar = new PasswordField();
        confirmar.setPromptText("Confirmar nueva contraseña");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.add(new Label("Actual:"), 0, 0);
        grid.add(actual, 1, 0);
        grid.add(new Label("Nueva:"), 0, 1);
        grid.add(nueva, 1, 1);
        grid.add(new Label("Confirmar:"), 0, 2);
        grid.add(confirmar, 1, 2);
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        if (!nueva.getText().equals(confirmar.getText())) {
            mostrarError("Las nuevas contraseñas no coinciden.");
            return;
        }

        try {
            authenticationService.changePassword(actual.getText(), nueva.getText());
            mostrarInformacion("Contraseña actualizada", "Tu contraseña fue cambiada correctamente.");
        } catch (Exception e) {
            mostrarError(e.getMessage() == null ? "No fue posible cambiar la contraseña." : e.getMessage());
        }
    }

    private void mostrarUsuarios() {
        Stage stage = new Stage();
        stage.setTitle("DentalCare | Usuarios");
        stage.setMinWidth(760);
        stage.setMinHeight(520);

        TableView<UserService.StoredUser> table = new TableView<>();
        TableColumn<UserService.StoredUser, String> usuario = new TableColumn<>("Usuario");
        usuario.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().username));
        TableColumn<UserService.StoredUser, String> nombre = new TableColumn<>("Nombre");
        nombre.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().displayName));
        TableColumn<UserService.StoredUser, String> rol = new TableColumn<>("Rol");
        rol.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().role.getDisplayName()));
        TableColumn<UserService.StoredUser, String> estado = new TableColumn<>("Estado");
        estado.setCellValueFactory(cell -> new ReadOnlyStringWrapper(cell.getValue().active ? "Activo" : "Inactivo"));
        table.getColumns().addAll(usuario, nombre, rol, estado);
        table.getColumns().forEach(column -> column.setPrefWidth(170));
        table.getItems().setAll(userService.listUsers());

        TextField nuevoUsuario = new TextField();
        nuevoUsuario.setPromptText("usuario");
        TextField nuevoNombre = new TextField();
        nuevoNombre.setPromptText("nombre visible");
        PasswordField nuevaPassword = new PasswordField();
        nuevaPassword.setPromptText("mínimo 8 caracteres");
        ComboBox<UserRole> nuevoRol = new ComboBox<>();
        nuevoRol.getItems().addAll(UserRole.ADMINISTRADOR, UserRole.USUARIO);
        nuevoRol.setValue(UserRole.USUARIO);

        Button crear = new Button("Crear usuario");
        crear.setOnAction(event -> {
            try {
                userService.createUser(nuevoUsuario.getText(), nuevoNombre.getText(), nuevaPassword.getText(), nuevoRol.getValue());
                table.getItems().setAll(userService.listUsers());
                nuevoUsuario.clear();
                nuevoNombre.clear();
                nuevaPassword.clear();
                mostrarInformacion("Usuario creado", "El usuario ya puede iniciar sesión.");
            } catch (Exception e) {
                mostrarError(e.getMessage() == null ? "No fue posible crear el usuario." : e.getMessage());
            }
        });

        Button cambiarEstado = new Button("Activar / desactivar");
        cambiarEstado.setOnAction(event -> {
            UserService.StoredUser selected = table.getSelectionModel().getSelectedItem();
            if (selected == null) {
                mostrarError("Selecciona un usuario primero.");
                return;
            }
            try {
                userService.setUserActive(selected.username, !selected.active);
                table.getItems().setAll(userService.listUsers());
            } catch (Exception e) {
                mostrarError(e.getMessage() == null ? "No fue posible cambiar el estado." : e.getMessage());
            }
        });

        GridPane form = new GridPane();
        form.setHgap(8);
        form.setVgap(8);
        form.add(new Label("Usuario:"), 0, 0);
        form.add(nuevoUsuario, 1, 0);
        form.add(new Label("Nombre:"), 2, 0);
        form.add(nuevoNombre, 3, 0);
        form.add(new Label("Contraseña:"), 0, 1);
        form.add(nuevaPassword, 1, 1);
        form.add(new Label("Rol:"), 2, 1);
        form.add(nuevoRol, 3, 1);
        form.add(crear, 4, 0);
        form.add(cambiarEstado, 4, 1);

        VBox root = new VBox(12, table, form);
        root.setPadding(new javafx.geometry.Insets(18));
        VBox.setVgrow(table, javafx.scene.layout.Priority.ALWAYS);
        stage.setScene(new Scene(root, 860, 560));
        stage.show();
    }

    private void mostrarRolesPermisos() {
        StringBuilder texto = new StringBuilder();
        for (UserRole role : UserRole.values()) {
            texto.append(role.getDisplayName()).append("\n");
            for (UserPermission permission : AuthenticatedUser.permissionsFor(role)) {
                texto.append("  • ").append(nombrePermiso(permission)).append("\n");
            }
            texto.append("\n");
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Roles y permisos");
        alert.setHeaderText("Permisos actuales de DentalCare");
        alert.setContentText(texto.toString());
        alert.showAndWait();
    }

    private void mostrarAuditoria() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Auditoría");
        alert.setHeaderText("Auditoría de seguridad");
        alert.setContentText("La auditoría detallada se habilitará como siguiente capa del sistema.\n\nLos roles y privilegios ya están activos.");
        alert.showAndWait();
    }

    private String nombrePermiso(UserPermission permission) {
        return switch (permission) {
            case VER_INICIO -> "Inicio";
            case VER_PACIENTES -> "Pacientes";
            case GESTIONAR_CITAS -> "Agenda y citas";
            case VER_TRATAMIENTOS -> "Tratamientos";
            case VER_HISTORIAL -> "Historial";
            case VER_FINANZAS -> "Finanzas";
            case VER_CONFIGURACION -> "Configuración";
            case GESTIONAR_USUARIOS -> "Gestión de usuarios";
            case VER_ROLES -> "Roles y permisos";
            case VER_AUDITORIA -> "Auditoría";
        };
    }

    private void cerrarSesion() {
        try {
            authenticationService.logout();
            Stage applicationStage = (Stage) contentArea.getScene().getWindow();
            applicationStage.close();

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ui/fxml/LoginView.fxml"));
            loader.setControllerFactory(context::getBean);
            Parent root = loader.load();
            Stage loginStage = new Stage();
            loginStage.initStyle(StageStyle.UNDECORATED);
            loginStage.setTitle("DentalCare | Iniciar sesión");
            loginStage.setScene(new Scene(root, 900, 540));
            loginStage.setResizable(false);
            loginStage.show();
            loginStage.centerOnScreen();
        } catch (Exception e) {
            mostrarError("No fue posible cerrar la sesión correctamente.");
        }
    }

    private void cargarVista(String ruta) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(ruta));
            loader.setControllerFactory(context::getBean);
            Parent view = loader.load();
            contentArea.getChildren().setAll(view);
        } catch (Exception e) {
            throw new RuntimeException("No fue posible cargar la vista: " + ruta, e);
        }
    }

    private void mostrarInformacion(String titulo, String mensaje) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }

    private void mostrarError(String mensaje) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("DentalCare");
        alert.setHeaderText(null);
        alert.setContentText(mensaje);
        alert.showAndWait();
    }
}
