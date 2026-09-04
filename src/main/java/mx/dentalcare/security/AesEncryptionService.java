package mx.dentalcare.security;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

public class AesEncryptionService {

    private static final int TAG_LENGTH = EncryptionConstants.GCM_TAG_LENGTH;

    public byte[] encrypt(byte[] data, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(EncryptionConstants.CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, key, spec);
            return cipher.doFinal(data);
        } catch (Exception e) {
            throw new RuntimeException("Error al cifrar datos.", e);
        }
    }

    public byte[] decrypt(byte[] encryptedData, SecretKey key, byte[] iv) {
        try {
            Cipher cipher = Cipher.getInstance(EncryptionConstants.CIPHER);
            GCMParameterSpec spec = new GCMParameterSpec(TAG_LENGTH, iv);
            cipher.init(Cipher.DECRYPT_MODE, key, spec);
            return cipher.doFinal(encryptedData);
        } catch (Exception e) {
            throw new RuntimeException("Error al descifrar datos.", e);
        }
    }
}
