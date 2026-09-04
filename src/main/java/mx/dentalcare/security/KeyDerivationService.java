package mx.dentalcare.security;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.SecretKey;

public class KeyDerivationService {
    public SecretKey deriveKey(String password, byte[] salt){
        try {
            PBEKeySpec spec = new PBEKeySpec(
                    password.toCharArray(),
                    salt,
                    EncryptionConstants.ITERATIONS,
                    EncryptionConstants.KEY_LENGTH
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(
                    EncryptionConstants.KEY_ALGORITHM
            );
            byte[] keyBytes = factory.generateSecret(spec).getEncoded();
            return new SecretKeySpec(keyBytes, "AES");

        }catch (Exception e){
            throw new RuntimeException("Error al derivar clave.");
        }
    }
}
