package mx.dentalcare.security;

import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final MasterKeyService masterKeyService;
    private final LegacyDataMigrationService migrationService;
    private final UserService userService;

    public AuthenticationService(
            MasterKeyService masterKeyService,
            LegacyDataMigrationService migrationService,
            UserService userService
    ) {
        this.masterKeyService = masterKeyService;
        this.migrationService = migrationService;
        this.userService = userService;
    }

    public boolean isConfigured() {
        return masterKeyService.isConfigured();
    }

    public boolean requiresLegacyMigration() {
        return !isConfigured() && migrationService.hasLegacyData();
    }

    public String setupAdmin(String password, String legacyPassword) {
        validatePassword(password);

        String recoveryKey = masterKeyService.initialize(password);

        try {
            migrationService.migrateIfNecessary(legacyPassword);
            userService.initializeAdmin(password);
            return recoveryKey;
        } catch (RuntimeException e) {
            masterKeyService.clearConfiguration();
            throw e;
        }
    }

    public AuthenticatedUser login(String username, String password) {
        validatePassword(password);
        return userService.authenticate(username, password);
    }

    public boolean hasRecoveryKey() {
        return masterKeyService.hasRecoveryKey();
    }

    public String generateRecoveryKey() {
        return masterKeyService.generateRecoveryKey();
    }

    /**
     * Restablece la contraseña del administrador usando exclusivamente
     * la clave de recuperación. La clave maestra y los datos clínicos no cambian.
     * Después del restablecimiento se genera una nueva clave de recuperación.
     */
    public String recoverAdminPassword(String recoveryKey, String newPassword) {
        validatePassword(newPassword);
        masterKeyService.unlockWithRecoveryKey(recoveryKey);
        userService.resetAdminPassword(newPassword);
        return masterKeyService.generateRecoveryKey();
    }

    public void logout() {
        masterKeyService.clearConfigurationSessionOnly();
    }

    public void changePassword(String currentPassword, String newPassword) {
        validatePassword(newPassword);
        userService.changeCurrentUserPassword(currentPassword, newPassword);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }
}
