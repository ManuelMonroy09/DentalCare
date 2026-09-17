package mx.dentalcare;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.KeyDerivationService;
import mx.dentalcare.security.MasterKeyService;
import mx.dentalcare.security.SecuritySession;
import mx.dentalcare.security.UserRole;
import mx.dentalcare.security.UserService;
import mx.dentalcare.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private SecuritySession securitySession;
    private UserService service;

    @BeforeEach
    void setUp() {
        securitySession = mock(SecuritySession.class);
        service = new UserService(
                new ObjectMapper(),
                new KeyDerivationService(),
                new AesEncryptionService(),
                mock(MasterKeyService.class),
                securitySession,
                mock(AuditService.class)
        );
    }

    @Test
    void usuarioNoAdministradorNoPuedeCrearUsuarios() {
        when(securitySession.requireCurrentUser())
                .thenReturn(new AuthenticatedUser("usuario", "Usuario", UserRole.USUARIO));

        assertThrows(SecurityException.class,
                () -> service.createUser("nuevo", "Nuevo Usuario", "password1", UserRole.USUARIO));
    }

    @Test
    void usuarioNoAdministradorNoPuedeListarUsuarios() {
        when(securitySession.requireCurrentUser())
                .thenReturn(new AuthenticatedUser("usuario", "Usuario", UserRole.USUARIO));

        assertThrows(SecurityException.class, service::listUsers);
    }

    @Test
    void usuarioNoAdministradorNoPuedeActualizarNombre() {
        when(securitySession.requireCurrentUser())
                .thenReturn(new AuthenticatedUser("usuario", "Usuario", UserRole.USUARIO));

        assertThrows(SecurityException.class,
                () -> service.updateDisplayName("admin", "Administrador"));
    }

    @Test
    void usuarioNoAdministradorNoPuedeActivarODesactivarCuentas() {
        when(securitySession.requireCurrentUser())
                .thenReturn(new AuthenticatedUser("usuario", "Usuario", UserRole.USUARIO));

        assertThrows(SecurityException.class, () -> service.setUserActive("admin", false));
    }

    @Test
    void cambioDePasswordDebeRechazarNuevaPasswordCortaAntesDeAutenticar() {
        assertThrows(IllegalArgumentException.class,
                () -> service.changeCurrentUserPassword("password1", "1234567"));
        verifyNoInteractions(securitySession);
    }

    @Test
    void recuperacionDebeRechazarNuevaPasswordCortaAntesDeLeerUsuarios() {
        assertThrows(IllegalArgumentException.class,
                () -> service.recoverPassword("usuario", "recovery", "1234567"));
        verifyNoInteractions(securitySession);
    }
}
