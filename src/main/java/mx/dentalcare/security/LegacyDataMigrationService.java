package mx.dentalcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.infrastructure.persistence.file.CitaData;
import mx.dentalcare.infrastructure.persistence.file.PacienteData;
import mx.dentalcare.infrastructure.persistence.file.TratamientoData;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

@Service
public class LegacyDataMigrationService {
    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;

    public LegacyDataMigrationService(ObjectMapper objectMapper, KeyDerivationService keyDerivationService,
                                      AesEncryptionService aesEncryptionService, SecuritySession securitySession) {
        this.storage = new EncryptedFileStorage(objectMapper, keyDerivationService, aesEncryptionService);
        this.securitySession = securitySession;
    }

    public boolean hasLegacyData() {
        return Files.exists(DataDirectoryService.resolve("pacientes.dat"))
                || Files.exists(DataDirectoryService.resolve("citas.dat"))
                || Files.exists(DataDirectoryService.resolve("tratamientos.dat"));
    }

    public void migrateIfNecessary(String legacyPassword) {
        if (!hasLegacyData()) return;
        if (legacyPassword == null || legacyPassword.isBlank()) {
            throw new IllegalArgumentException("Se requiere la contraseña anterior para migrar los datos existentes.");
        }
        SecretKey masterKey = securitySession.requireMasterKey();
        List<MigrationEntry<?>> entries = new ArrayList<>();
        prepare(DataDirectoryService.resolve("pacientes.dat"), PacienteData.class, legacyPassword, masterKey, entries);
        prepare(DataDirectoryService.resolve("citas.dat"), CitaData.class, legacyPassword, masterKey, entries);
        prepare(DataDirectoryService.resolve("tratamientos.dat"), TratamientoData.class, legacyPassword, masterKey, entries);
        try {
            for (MigrationEntry<?> entry : entries) entry.createBackup();
            for (MigrationEntry<?> entry : entries) entry.replaceOriginal();
            for (MigrationEntry<?> entry : entries) entry.deleteBackup();
        } catch (Exception e) {
            for (MigrationEntry<?> entry : entries) try { entry.restoreBackup(); } catch (Exception ignored) { }
            throw new RuntimeException("No fue posible completar la migración de los datos existentes.", e);
        } finally {
            for (MigrationEntry<?> entry : entries) try { entry.deleteTemporary(); } catch (Exception ignored) { }
        }
    }

    private <T> void prepare(Path path, Class<T> type, String legacyPassword, SecretKey masterKey, List<MigrationEntry<?>> entries) {
        if (!Files.exists(path)) return;
        T data = storage.load(path, type, legacyPassword);
        if (data == null) return;
        MigrationEntry<T> entry = new MigrationEntry<>(path, type, data, masterKey);
        entry.createTemporaryAndVerify(storage);
        entries.add(entry);
    }

    private static final class MigrationEntry<T> {
        private final Path original;
        private final Class<T> type;
        private final T data;
        private final SecretKey masterKey;
        private final Path temporary;
        private final Path backup;

        private MigrationEntry(Path original, Class<T> type, T data, SecretKey masterKey) {
            this.original = original;
            this.type = type;
            this.data = data;
            this.masterKey = masterKey;
            this.temporary = original.resolveSibling(original.getFileName() + ".migration.tmp");
            this.backup = original.resolveSibling(original.getFileName() + ".migration.bak");
        }
        private void createTemporaryAndVerify(EncryptedFileStorage storage) {
            try {
                storage.save(temporary, data, masterKey);
                T verification = storage.load(temporary, type, masterKey);
                if (verification == null) throw new IllegalStateException("No fue posible verificar la migración de " + original.getFileName());
            } catch (Exception e) {
                try { Files.deleteIfExists(temporary); } catch (Exception ignored) { }
                throw new RuntimeException("No fue posible preparar la migración de " + original.getFileName(), e);
            }
        }
        private void createBackup() throws Exception { Files.copy(original, backup, StandardCopyOption.REPLACE_EXISTING); }
        private void replaceOriginal() throws Exception {
            try { Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, original, StandardCopyOption.REPLACE_EXISTING); }
        }
        private void restoreBackup() throws Exception { if (Files.exists(backup)) Files.copy(backup, original, StandardCopyOption.REPLACE_EXISTING); }
        private void deleteBackup() throws Exception { Files.deleteIfExists(backup); }
        private void deleteTemporary() throws Exception { Files.deleteIfExists(temporary); }
    }
}
