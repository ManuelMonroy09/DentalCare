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

    public EncryptedFileStorage(ObjectMapper objectMapper,
                                KeyDerivationService keyDerivationService1,
                                AesEncryptionService aesEncryptionService){
     this.objectMapper = objectMapper;
     this.keyDerivationService = keyDerivationService1;
     this.aesEncryptionService = aesEncryptionService;
    }

    public <T> void save(Path path, T data, String password){
        try{
            byte[] jsonBytes = objectMapper.writeValueAsBytes(data);
            byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
            byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
            SecretKey key = keyDerivationService.deriveKey(password,salt);
            byte[] encypted = aesEncryptionService.encrypt(jsonBytes,key,iv);
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(salt);
            output.write(iv);
            output.write(encypted);
            Files.createDirectories(path.getParent());
            Files.write(path, output.toByteArray());
        }catch(Exception e){
            throw new RuntimeException("No fue posible guardar el archivo cifrado.", e);
        }
    }

    public <T> T load(Path path, Class<T> type, String password){
        try{
            if(!Files.exists(path)){
                return null;
            }
            byte[] fileBytes = Files.readAllBytes(path);
            byte[] salt = extractSalt(fileBytes);
            byte[] iv = extractIv(fileBytes);
            byte[] encrypted = extractCiphertext(fileBytes);
            SecretKey key = keyDerivationService.deriveKey(password, salt);
            byte[] jsonBytes = aesEncryptionService.decrypt(encrypted, key, iv);
            return objectMapper.readValue(jsonBytes, type);
        }catch (Exception e){
            throw new RuntimeException("No fue posible leer el archivo cifrado.", e);
        }
    }
    private byte[] extractSalt(byte[] fileBytes) {
        return Arrays.copyOfRange(fileBytes, 0, EncryptionConstants.SALT_LENGTH);
    }
    private byte[] extractIv(byte[] fileBytes) {
        return Arrays.copyOfRange(fileBytes, EncryptionConstants.SALT_LENGTH, EncryptionConstants.SALT_LENGTH + EncryptionConstants.IV_LENGTH);
    }
    private byte[] extractCiphertext(byte[] fileBytes) {
        return Arrays.copyOfRange(fileBytes, EncryptionConstants.SALT_LENGTH + EncryptionConstants.IV_LENGTH, fileBytes.length);
    }
}