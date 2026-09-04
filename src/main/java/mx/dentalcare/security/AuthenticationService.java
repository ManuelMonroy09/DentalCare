package mx.dentalcare.security;

import org.springframework.stereotype.Service;

@Service
public class AuthenticationService {

    private final MasterKeyService masterKeyService;
    private final LegacyDataMigrationService migrationService;

    public AuthenticationService(
            MasterKeyService masterKeyService,
            LegacyDataMigrationService migrationService
    ) {
        this.masterKeyService = masterKeyService;
        this.migrationService = migrationService;
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
        } catch (RuntimeException e) {
            masterKeyService.clearConfiguration();
            throw e;
        }
    }

    public void login(String password) {
        validatePassword(password);
        masterKeyService.unlock(password);
    }

    public void logout() {
        // La sesión se limpia desde MasterKeyService mediante unlock failure o
        // desde el controlador al cerrar la aplicación. Se mantiene aquí como
        // punto único para la API de autenticación.
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }
}
