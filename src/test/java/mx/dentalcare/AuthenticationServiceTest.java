package mx.dentalcare;

import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.AuthenticationService;
import mx.dentalcare.security.LegacyDataMigrationService;
import mx.dentalcare.security.MasterKeyService;
import mx.dentalcare.security.UserRole;
import mx.dentalcare.security.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuthenticationServiceTest {

    private MasterKeyService masterKeyService;
    private LegacyDataMigrationService migrationService;
    private UserService userService;
    private AuthenticationService service;

    @BeforeEach
    void setUp() {
        masterKeyService = mock(MasterKeyService.class);
        migrationService = mock(LegacyDataMigrationService.class);
        userService = mock(UserService.class);
        service = new AuthenticationService(masterKeyService, migrationService, userService);
    }

    @Test
    void debeIndicarSiLaSeguridadEstaConfigurada() {
        when(masterKeyService.isConfigured()).thenReturn(true);
        assertTrue(service.isConfigured());
        verify(masterKeyService).isConfigured();
    }

    @Test
    void debeDetectarMigracionLegacySoloSiNoEstaConfigurado() {
        when(masterKeyService.isConfigured()).thenReturn(false);
        when(migrationService.hasLegacyData()).thenReturn(true);
        assertTrue(service.requiresLegacyMigration());

        when(masterKeyService.isConfigured()).thenReturn(true);
        assertFalse(service.requiresLegacyMigration());
        verify(migrationService, times(1)).hasLegacyData();
    }

    @Test
    void debeRechazarPasswordCortaAlConfigurar() {
        assertThrows(IllegalArgumentException.class, () -> service.setupAdmin("1234567", null));
        verifyNoInteractions(masterKeyService, migrationService, userService);
    }

    @Test
    void debeDelegarConfiguracionDelAdministrador() {
        when(masterKeyService.initialize("password1")).thenReturn("recovery-key");

        String result = service.setupAdmin("password1", "legacy");

        assertEquals("recovery-key", result);
        verify(masterKeyService).initialize("password1");
        verify(migrationService).migrateIfNecessary("legacy");
        verify(userService).initializeAdmin("password1", "recovery-key");
    }

    @Test
    void debeLimpiarConfiguracionSiFallaLaInicializacion() {
        when(masterKeyService.initialize("password1")).thenReturn("recovery-key");
        doThrow(new IllegalStateException("fallo")).when(userService).initializeAdmin("password1", "recovery-key");

        assertThrows(IllegalStateException.class, () -> service.setupAdmin("password1", null));
        verify(masterKeyService).clearConfiguration();
    }

    @Test
    void debeDelegarLogin() {
        AuthenticatedUser user = new AuthenticatedUser("juan", "Juan", UserRole.USUARIO);
        when(userService.authenticate("juan", "password1")).thenReturn(user);

        assertSame(user, service.login("juan", "password1"));
        verify(userService).authenticate("juan", "password1");
    }

    @Test
    void debeRechazarPasswordCortaAlIniciarSesion() {
        assertThrows(IllegalArgumentException.class, () -> service.login("juan", "1234567"));
        verifyNoInteractions(userService);
    }

    @Test
    void debeUsarRecuperacionPorCuentaCuandoExiste() {
        when(userService.hasRecoveryKey("juan")).thenReturn(true);
        when(userService.recoverPassword("juan", "recovery", "password2")).thenReturn("new-recovery");

        assertEquals("new-recovery", service.recoverPassword("juan", "recovery", "password2"));
        verify(userService).recoverPassword("juan", "recovery", "password2");
        verify(masterKeyService, never()).unlockWithRecoveryKey(anyString());
    }

    @Test
    void debeRechazarRecuperacionDeCuentaSinClave() {
        when(userService.hasRecoveryKey("juan")).thenReturn(false);
        assertThrows(SecurityException.class, () -> service.recoverPassword("juan", "recovery", "password2"));
    }

    @Test
    void debeRechazarPasswordNuevaCortaEnRecuperacion() {
        assertThrows(IllegalArgumentException.class, () -> service.recoverPassword("juan", "recovery", "1234567"));
        verifyNoInteractions(userService, masterKeyService);
    }
}
