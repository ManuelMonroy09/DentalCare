package mx.dentalcare.security;

public final class EncryptionConstants {

    private EncryptionConstants() {
    }

    public static final int SALT_LENGTH = 16;
    public static final int IV_LENGTH = 12;
    public static final int KEY_LENGTH = 256;

    /** KDF utilizado por el esquema actual. */
    public static final int KDF_ITERATIONS = 600_000;

    /** Solo para leer archivos creados por el esquema anterior. */
    public static final int LEGACY_ITERATIONS = 65_536;

    public static final String KEY_ALGORITHM = "PBKDF2WithHmacSHA256";
    public static final String CIPHER = "AES/GCM/NoPadding";

    public static final int GCM_TAG_LENGTH = 128;

    public static final int MASTER_KEY_LENGTH_BYTES = 32;

    public static final int SECURITY_VERSION = 1;
}
