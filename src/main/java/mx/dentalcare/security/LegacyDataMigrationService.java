package mx.dentalcare.security;

import mx.dentalcare.infrastructure.persistence.file.CitaData;
import mx.dentalcare.infrastructure.persistence.file.PacienteData;
import mx.dentalcare.infrastructure.persistence.file.TratamientoData;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

@Service
public class LegacyDataMigrationService {

    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;

    public LegacyDataMigrationService(
            ObjectMapper objectMapper,
            KeyDerivationService keyDerivationService,
            AesEncryptionService aesEncryptionService,
            SecuritySession securitySession
    ) {
        this.storage = new EncryptedFileStorage(
                objectMapper,
                keyDerivationService,
                aesEncryptionService
        );
        this.securitySession = securitySession;
    }

    public boolean hasLegacyData() {
        return Files.exists(Path.of("data", "pacientes.dat"))
                || Files.exists(Path.of("data", "citas.dat"))
                || Files.exists(Path.of("data", "tratamientos.dat"));
    }

    public void migrateIfNecessary(String legacyPassword) {
        if (!hasLegacyData()) {
            return;
        }

        if (legacyPassword == null || legacyPassword.isBlank()) {
            throw new IllegalArgumentException("Se requiere la contraseña anterior para migrar los datos existentes.");
        }

        SecretKey masterKey = securitySession.requireMasterKey();

        migrate(Path.of("data", "pacientes.dat"), PacienteData.class, legacyPassword, masterKey);
        migrate(Path.of("data", "citas.dat"), CitaData.class, legacyPassword, masterKey);
        migrate(Path.of("data", "tratamientos.dat"), TratamientoData.class, legacyPassword, masterKey);
    }

    private <T> void migrate(
            Path path,
            Class<T> type,
            String legacyPassword,
            SecretKey masterKey
    ) {
        if (!Files.exists(path)) {
            return;
        }

        T data = storage.load(path, type, legacyPassword);
        if (data == null) {
            return;
        }

        Path temporaryPath = path.resolveSibling(path.getFileName() + ".migration.tmp");

        try {
            storage.save(temporaryPath, data, masterKey);

            T verification = storage.load(temporaryPath, type, masterKey);
            if (verification == null) {
                throw new IllegalStateException(
                        "No fue posible verificar la migración de " + path.getFileName()
                );
            }

            try {
                Files.move(
                        temporaryPath,
                        path,
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(
                        temporaryPath,
                        path,
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception e) {
            try {
                Files.deleteIfExists(temporaryPath);
            } catch (Exception ignored) {
                // Conservamos el error original.
            }

            throw new RuntimeException(
                    "No fue posible migrar " + path.getFileName() + ". Verifica la contraseña anterior.",
                    e
            );
        }
    }
}
