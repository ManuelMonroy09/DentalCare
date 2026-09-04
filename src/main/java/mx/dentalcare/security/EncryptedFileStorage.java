package mx.dentalcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

public class EncryptedFileStorage {

    private final ObjectMapper objectMapper;
    private final KeyDerivationService keyDerivationService;
    private final AesEncryptionService aesEncryptionService;

    public EncryptedFileStorage(
            ObjectMapper objectMapper,
            KeyDerivationService keyDerivationService,
            AesEncryptionService aesEncryptionService
    ) {
        this.objectMapper = objectMapper;
        this.keyDerivationService = keyDerivationService;
        this.aesEncryptionService = aesEncryptionService;
    }

    /**
     * Esquema legado. Se conserva para compatibilidad durante la migración.
     */
    public <T> void save(Path path, T data, String password) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
            byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
            byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
            SecretKey key = keyDerivationService.deriveLegacyKey(password, salt);
            byte[] encrypted = aesEncryptionService.encrypt(jsonBytes, key, iv);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(salt);
            output.write(iv);
            output.write(encrypted);

            Files.createDirectories(path.getParent());
            Files.write(path, output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("No fue posible guardar el archivo cifrado.", e);
        }
    }

    /**
     * Guarda usando directamente la clave maestra de DentalCare.
     * La contraseña del administrador nunca llega a este método.
     */
    public <T> void save(Path path, T data, SecretKey key) {
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
            byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
            byte[] encrypted = aesEncryptionService.encrypt(jsonBytes, key, iv);

            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(iv);
            output.write(encrypted);

            Files.createDirectories(path.getParent());
            Files.write(path, output.toByteArray());
        } catch (Exception e) {
            throw new RuntimeException("No fue posible guardar el archivo cifrado.", e);
        }
    }

    /**
     * Lee archivos creados por el esquema anterior.
     */
    public <T> T load(Path path, Class<T> type, String password) {
        try {
            if (!Files.exists(path)) {
                return null;
            }

            byte[] fileBytes = Files.readAllBytes(path);
            validateFile(fileBytes, EncryptionConstants.SALT_LENGTH + EncryptionConstants.IV_LENGTH + 16);

            byte[] salt = extractSalt(fileBytes);
            byte[] iv = extractLegacyIv(fileBytes);
            byte[] encrypted = extractLegacyCiphertext(fileBytes);
            SecretKey key = keyDerivationService.deriveLegacyKey(password, salt);
            byte[] jsonBytes = aesEncryptionService.decrypt(encrypted, key, iv);

            return objectMapper.readValue(jsonBytes, type);
        } catch (Exception e) {
            throw new RuntimeException("No fue posible leer el archivo cifrado.", e);
        }
    }

    /**
     * Lee archivos creados con la clave maestra.
     */
    public <T> T load(Path path, Class<T> type, SecretKey key) {
        try {
            if (!Files.exists(path)) {
                return null;
            }

            byte[] fileBytes = Files.readAllBytes(path);
            validateFile(fileBytes, EncryptionConstants.IV_LENGTH + 16);

            byte[] iv = Arrays.copyOfRange(fileBytes, 0, EncryptionConstants.IV_LENGTH);
            byte[] encrypted = Arrays.copyOfRange(fileBytes, EncryptionConstants.IV_LENGTH, fileBytes.length);
            byte[] jsonBytes = aesEncryptionService.decrypt(encrypted, key, iv);

            return objectMapper.readValue(jsonBytes, type);
        } catch (Exception e) {
            throw new RuntimeException("No fue posible leer el archivo cifrado.", e);
        }
    }

    private void validateFile(byte[] fileBytes, int minimumLength) {
        if (fileBytes == null || fileBytes.length < minimumLength) {
            throw new IllegalArgumentException("El archivo cifrado está incompleto o corrupto.");
        }
    }

    private byte[] extractSalt(byte[] fileBytes) {
        return Arrays.copyOfRange(fileBytes, 0, EncryptionConstants.SALT_LENGTH);
    }

    private byte[] extractLegacyIv(byte[] fileBytes) {
        return Arrays.copyOfRange(
                fileBytes,
                EncryptionConstants.SALT_LENGTH,
                EncryptionConstants.SALT_LENGTH + EncryptionConstants.IV_LENGTH
        );
    }

    private byte[] extractLegacyCiphertext(byte[] fileBytes) {
        int start = EncryptionConstants.SALT_LENGTH + EncryptionConstants.IV_LENGTH;
        return Arrays.copyOfRange(fileBytes, start, fileBytes.length);
    }
}
