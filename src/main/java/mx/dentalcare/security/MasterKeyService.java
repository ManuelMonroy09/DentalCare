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

    public boolean isConfigured() { return Files.exists(SECURITY_FILE); }

    public synchronized void initialize(String password) {
        if (isConfigured()) throw new IllegalStateException("La seguridad de DentalCare ya está configurada.");
        SecretKey masterKey = generateMasterKey();
        AuthenticationData metadata = wrapMasterKey(masterKey, password);
        writeMetadata(metadata);
        securitySession.authenticate(masterKey);
    }

    public synchronized void unlock(String password) {
        if (!isConfigured()) throw new IllegalStateException("La seguridad de DentalCare no está configurada.");
        try {
            AuthenticationData metadata = objectMapper.readValue(Files.readString(SECURITY_FILE), AuthenticationData.class);
            validateMetadata(metadata);
            byte[] salt = Base64.getDecoder().decode(metadata.getSalt());
            byte[] iv = Base64.getDecoder().decode(metadata.getIv());
            byte[] wrappedMasterKey = Base64.getDecoder().decode(metadata.getWrappedMasterKey());
            SecretKey protectionKey = keyDerivationService.deriveKey(password, salt);
            byte[] masterKeyBytes = aesEncryptionService.decrypt(wrappedMasterKey, protectionKey, iv);
            if (masterKeyBytes.length != EncryptionConstants.MASTER_KEY_LENGTH_BYTES) {
                throw new SecurityException("La clave maestra tiene un tamaño inválido.");
            }
            securitySession.authenticate(new SecretKeySpec(masterKeyBytes, "AES"));
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("Contraseña incorrecta o configuración de seguridad inválida.", e);
        }
    }

    public synchronized void changePassword(String currentPassword, String newPassword) {
        unlock(currentPassword);
        SecretKey masterKey = securitySession.requireMasterKey();
        writeMetadata(wrapMasterKey(masterKey, newPassword));
    }

    public synchronized void clearConfiguration() {
        securitySession.clear();
        try { Files.deleteIfExists(SECURITY_FILE); }
        catch (Exception e) { throw new RuntimeException("No fue posible eliminar la configuración de seguridad.", e); }
    }

    private SecretKey generateMasterKey() {
        byte[] bytes = new byte[EncryptionConstants.MASTER_KEY_LENGTH_BYTES];
        secureRandom.nextBytes(bytes);
        return new SecretKeySpec(bytes, "AES");
    }

    private AuthenticationData wrapMasterKey(SecretKey masterKey, String password) {
        validatePassword(password);
        byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
        byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
        SecretKey protectionKey = keyDerivationService.deriveKey(password, salt);
        byte[] wrapped = aesEncryptionService.encrypt(masterKey.getEncoded(), protectionKey, iv);
        return new AuthenticationData(EncryptionConstants.SECURITY_VERSION, EncryptionConstants.KDF_ITERATIONS,
                Base64.getEncoder().encodeToString(salt), Base64.getEncoder().encodeToString(iv),
                Base64.getEncoder().encodeToString(wrapped));
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
        } catch (Exception e) { throw new RuntimeException("No fue posible guardar la configuración de seguridad.", e); }
    }

    private void validateMetadata(AuthenticationData metadata) {
        if (metadata == null || metadata.getVersion() != EncryptionConstants.SECURITY_VERSION
                || metadata.getKdfIterations() != EncryptionConstants.KDF_ITERATIONS
                || metadata.getSalt() == null || metadata.getIv() == null || metadata.getWrappedMasterKey() == null) {
            throw new SecurityException("La configuración de seguridad no es válida.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
    }
}
