package mx.dentalcare;

import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.UserPermission;
import mx.dentalcare.security.UserRole;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class UserPermissionTest {

    @Test
    void administradorDebeTenerTodosLosPermisos() {
        AuthenticatedUser admin = new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR);

        assertTrue(admin.isAdmin());
        assertEquals(Set.copyOf(UserPermission.values()), AuthenticatedUser.permissionsFor(UserRole.ADMINISTRADOR));
        for (UserPermission permission : UserPermission.values()) {
            assertTrue(admin.hasPermission(permission));
        }
    }

    @Test
    void usuarioDebeTenerSoloPermisosOperativosDefinidos() {
        AuthenticatedUser user = new AuthenticatedUser("usuario", "Usuario", UserRole.USUARIO);

        assertFalse(user.isAdmin());
        assertTrue(user.hasPermission(UserPermission.VER_INICIO));
        assertTrue(user.hasPermission(UserPermission.VER_PACIENTES));
        assertTrue(user.hasPermission(UserPermission.GESTIONAR_CITAS));
        assertTrue(user.hasPermission(UserPermission.VER_FINANZAS));

        assertFalse(user.hasPermission(UserPermission.VER_TRATAMIENTOS));
        assertFalse(user.hasPermission(UserPermission.VER_HISTORIAL));
        assertFalse(user.hasPermission(UserPermission.VER_CONFIGURACION));
        assertFalse(user.hasPermission(UserPermission.GESTIONAR_USUARIOS));
        assertFalse(user.hasPermission(UserPermission.VER_ROLES));
        assertFalse(user.hasPermission(UserPermission.VER_AUDITORIA));
    }
}
