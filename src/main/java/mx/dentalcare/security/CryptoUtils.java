package mx.dentalcare.security;

import java.security.SecureRandom;

public final class CryptoUtils {

    private static final SecureRandom RANDOM = new SecureRandom();
    private CryptoUtils(){}
    public static byte[] randomBytes(int size){
        byte[] bytes = new byte[size];
        RANDOM.nextBytes(bytes);
        return bytes;
    }
}
