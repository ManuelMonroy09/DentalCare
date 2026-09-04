package mx.dentalcare.security;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class KeyDerivationService {

    public SecretKey deriveKey(String password, byte[] salt) {
        return deriveKey(password, salt, EncryptionConstants.KDF_ITERATIONS);
    }

    public SecretKey deriveLegacyKey(String password, byte[] salt) {
        return deriveKey(password, salt, EncryptionConstants.LEGACY_ITERATIONS);
    }

    private SecretKey deriveKey(String password, byte[] salt, int iterations) {
        if (password == null || password.isBlank()) {
            throw new IllegalArgumentException("La contraseña no puede estar vacía.");
        }

        if (salt == null || salt.length == 0) {
            throw new IllegalArgumentException("El salt no puede estar vacío.");
        }

        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    iterations,
                    EncryptionConstants.KEY_LENGTH
            );

            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    EncryptionConstants.KEY_ALGORITHM
            );

            byte[] keyBytes;
            try {
                keyBytes = factory.generateSecret(spec).getEncoded();
            } finally {
                spec.clearPassword();
            }

            return new SecretKeySpec(keyBytes, "AES");

        } catch (Exception e) {
            throw new RuntimeException("Error al derivar clave.", e);
        }
    }
}
