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

    public void setupAdmin(String password, String legacyPassword) {
        validatePassword(password);

        masterKeyService.initialize(password);

        try {
            migrationService.migrateIfNecessary(legacyPassword);
            userService.initializeAdmin(password);
        } catch (RuntimeException e) {
            masterKeyService.clearConfiguration();
            throw e;
        }
    }

    public AuthenticatedUser login(String username, String password) {
        validatePassword(password);
        return userService.authenticate(username, password);
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
