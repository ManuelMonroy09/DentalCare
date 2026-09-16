package mx.dentalcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.Base64;

@Service
public class MasterKeyService {

    private static final Path SECURITY_FILE = DataDirectoryService.resolve("security.dat");

    private final ObjectMapper objectMapper;
    private final KeyDerivationService keyDerivationService;
    private final AesEncryptionService aesEncryptionService;
    private final SecuritySession securitySession;
    private final SecureRandom secureRandom = new SecureRandom();

    public MasterKeyService(ObjectMapper objectMapper, KeyDerivationService keyDerivationService,
                             AesEncryptionService aesEncryptionService, SecuritySession securitySession) {
        this.objectMapper = objectMapper;
        this.keyDerivationService = keyDerivationService;
        this.aesEncryptionService = aesEncryptionService;
        this.securitySession = securitySession;
    }

    public boolean isConfigured() {
        return Files.exists(SECURITY_FILE);
    }

    public synchronized String initialize(String password) {
        if (isConfigured()) {
            throw new IllegalStateException("La seguridad de DentalCare ya está configurada.");
        }

        SecretKey masterKey = generateMasterKey();
        AuthenticationData metadata = wrapMasterKey(masterKey, password);
        String recoveryKey = configureRecoveryKey(metadata, masterKey);
        writeMetadata(metadata);
        securitySession.authenticate(masterKey);
        return recoveryKey;
    }

    public synchronized void unlock(String password) {
        if (!isConfigured()) {
            throw new IllegalStateException("La seguridad de DentalCare no está configurada.");
        }

        try {
            AuthenticationData metadata = readMetadata();
            validateMetadata(metadata);
            securitySession.authenticate(unwrapMasterKey(metadata, password));
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("Contraseña incorrecta o configuración de seguridad inválida.", e);
        }
    }

    public synchronized boolean hasRecoveryKey() {
        if (!isConfigured()) return false;
        try {
            AuthenticationData metadata = readMetadata();
            return metadata.getRecoverySalt() != null
                    && metadata.getRecoveryIv() != null
                    && metadata.getRecoveryWrappedMasterKey() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public synchronized String generateRecoveryKey() {
        SecretKey masterKey = securitySession.requireMasterKey();
        AuthenticationData metadata = readMetadata();
        String recoveryKey = configureRecoveryKey(metadata, masterKey);
        writeMetadata(metadata);
        return recoveryKey;
    }

    public synchronized void unlockWithRecoveryKey(String recoveryKey) {
        if (!isConfigured()) {
            throw new IllegalStateException("La seguridad de DentalCare no está configurada.");
        }
        if (recoveryKey == null || recoveryKey.isBlank()) {
            throw new SecurityException("La clave de recuperación es obligatoria.");
        }

        try {
            AuthenticationData metadata = readMetadata();
            validateMetadata(metadata);
            if (metadata.getRecoverySalt() == null || metadata.getRecoveryIv() == null
                    || metadata.getRecoveryWrappedMasterKey() == null) {
                throw new SecurityException("La recuperación del administrador todavía no está configurada.");
            }

            byte[] salt = Base64.getDecoder().decode(metadata.getRecoverySalt());
            byte[] iv = Base64.getDecoder().decode(metadata.getRecoveryIv());
            byte[] wrappedMasterKey = Base64.getDecoder().decode(metadata.getRecoveryWrappedMasterKey());
            SecretKey recoveryProtectionKey = keyDerivationService.deriveKey(normalizeRecoveryKey(recoveryKey), salt);
            byte[] masterKeyBytes = aesEncryptionService.decrypt(wrappedMasterKey, recoveryProtectionKey, iv);

            if (masterKeyBytes.length != EncryptionConstants.MASTER_KEY_LENGTH_BYTES) {
                throw new SecurityException("La clave maestra tiene un tamaño inválido.");
            }

            securitySession.authenticate(new SecretKeySpec(masterKeyBytes, "AES"));
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("La clave de recuperación es incorrecta o no es válida.", e);
        }
    }

    public synchronized void changePassword(String currentPassword, String newPassword) {
        unlock(currentPassword);
        SecretKey masterKey = securitySession.requireMasterKey();
        AuthenticationData current = readMetadata();
        AuthenticationData passwordMetadata = wrapMasterKey(masterKey, newPassword);
        copyRecoveryData(current, passwordMetadata);
        writeMetadata(passwordMetadata);
    }

    public synchronized void clearConfiguration() {
        securitySession.clear();
        try {
            Files.deleteIfExists(SECURITY_FILE);
        } catch (Exception e) {
            throw new RuntimeException("No fue posible eliminar la configuración de seguridad.", e);
        }
    }

    public synchronized void clearConfigurationSessionOnly() {
        securitySession.clear();
    }

    private SecretKey generateMasterKey() {
        byte[] bytes = new byte[EncryptionConstants.MASTER_KEY_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);
        return new SecretKeySpec(bytes, "AES");
    }

    private SecretKey unwrapMasterKey(AuthenticationData metadata, String password) {
        byte[] salt = Base64.getDecoder().decode(metadata.getSalt());
        byte[] iv = Base64.getDecoder().decode(metadata.getIv());
        byte[] wrappedMasterKey = Base64.getDecoder().decode(metadata.getWrappedMasterKey());
        SecretKey protectionKey = keyDerivationService.deriveKey(password, salt);
        byte[] masterKeyBytes = aesEncryptionService.decrypt(wrappedMasterKey, protectionKey, iv);

        if (masterKeyBytes.length != EncryptionConstants.MASTER_KEY_LENGTH_BYTES) {
            throw new SecurityException("La clave maestra tiene un tamaño inválido.");
        }
        return new SecretKeySpec(masterKeyBytes, "AES");
    }

    private AuthenticationData wrapMasterKey(SecretKey masterKey, String password) {
        validatePassword(password);
        byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
        byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
        SecretKey protectionKey = keyDerivationService.deriveKey(password, salt);
        byte[] wrapped = aesEncryptionService.encrypt(masterKey.getEncoded(), protectionKey, iv);
        return new AuthenticationData(
                EncryptionConstants.SECURITY_VERSION,
                EncryptionConstants.KDF_ITERATIONS,
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(wrapped)
        );
    }

    private String configureRecoveryKey(AuthenticationData metadata, SecretKey masterKey) {
        byte[] recoveryBytes = new byte[EncryptionConstants.MASTER_KEY_LENGTH_BYTES];
        secureRandom.nextBytes(recoveryBytes);
        String recoveryKey = Base64.getUrlEncoder().withoutPadding().encodeToString(recoveryBytes);

        byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
        byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
        SecretKey recoveryProtectionKey = keyDerivationService.deriveKey(recoveryKey, salt);
        byte[] wrappedMasterKey = aesEncryptionService.encrypt(masterKey.getEncoded(), recoveryProtectionKey, iv);

        metadata.setRecoverySalt(Base64.getEncoder().encodeToString(salt));
        metadata.setRecoveryIv(Base64.getEncoder().encodeToString(iv));
        metadata.setRecoveryWrappedMasterKey(Base64.getEncoder().encodeToString(wrappedMasterKey));
        return formatRecoveryKey(recoveryKey);
    }

    /**
     * La clave mostrada se agrupa con guiones cada 8 caracteres. Los guiones
     * también son válidos dentro de Base64 URL-safe, por lo que no se pueden
     * eliminar globalmente. Aquí se quitan únicamente los separadores que
     * insertamos en posiciones conocidas, conservando cualquier '-' original.
     */
    private String normalizeRecoveryKey(String recoveryKey) {
        String value = recoveryKey.trim().replaceAll("\\s+", "");
        int rawLength = 43;
        int formattedLength = 48;
        if (value.length() == formattedLength) {
            StringBuilder raw = new StringBuilder(rawLength);
            for (int i = 0; i < value.length(); i++) {
                if (i == 8 || i == 17 || i == 26 || i == 35 || i == 44) {
                    if (value.charAt(i) != '-') {
                        return value;
                    }
                    continue;
                }
                raw.append(value.charAt(i));
            }
            if (raw.length() == rawLength) return raw.toString();
        }
        return value;
    }

    private String formatRecoveryKey(String recoveryKey) {
        StringBuilder formatted = new StringBuilder();
        for (int i = 0; i < recoveryKey.length(); i++) {
            if (i > 0 && i % 8 == 0) formatted.append('-');
            formatted.append(recoveryKey.charAt(i));
        }
        return formatted.toString();
    }

    private AuthenticationData readMetadata() {
        try {
            return objectMapper.readValue(Files.readString(SECURITY_FILE), AuthenticationData.class);
        } catch (Exception e) {
            throw new SecurityException("No fue posible leer la configuración de seguridad.", e);
        }
    }

    private void copyRecoveryData(AuthenticationData source, AuthenticationData target) {
        target.setRecoverySalt(source.getRecoverySalt());
        target.setRecoveryIv(source.getRecoveryIv());
        target.setRecoveryWrappedMasterKey(source.getRecoveryWrappedMasterKey());
    }

    private void writeMetadata(AuthenticationData metadata) {
        try {
            Files.createDirectories(SECURITY_FILE.getParent());
            Path temporaryFile = SECURITY_FILE.resolveSibling("security.dat.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporaryFile.toFile(), metadata);
            try {
                Files.move(temporaryFile, SECURITY_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporaryFile, SECURITY_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new RuntimeException("No fue posible guardar la configuración de seguridad.", e);
        }
    }

    private void validateMetadata(AuthenticationData metadata) {
        if (metadata == null || metadata.getVersion() != EncryptionConstants.SECURITY_VERSION
                || metadata.getKdfIterations() != EncryptionConstants.KDF_ITERATIONS
                || metadata.getSalt() == null || metadata.getIv() == null
                || metadata.getWrappedMasterKey() == null) {
            throw new SecurityException("La configuración de seguridad no es válida.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }
}
