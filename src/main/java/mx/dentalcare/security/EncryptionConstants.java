package mx.dentalcare.security;

public class EncryptionConstants {
    private EncryptionConstants(){}
    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 12;
    public static final int KEY_LENGTH = 256;
    public static final int ITERATIONS= 65536;
    public static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final String CIPHER = "AES/GCM/NoPadding";
}