package mx.dentalcare;

import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.CryptoUtils;
import mx.dentalcare.security.KeyDerivationService;
import mx.dentalcare.security.UserPermission;
import mx.dentalcare.security.UserRole;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class SecurityPrimitivesTest {

    @Test
    void aesDebeCifrarYDescifrar() {
        AesEncryptionService aes = new AesEncryptionService();
        SecretKey key = new SecretKeySpec(new byte[32], "AES");
        byte[] iv = new byte[12];
        byte[] original = "DentalCare seguridad".getBytes(StandardCharsets.UTF_8);

        byte[] encrypted = aes.encrypt(original, key, iv);
        byte[] decrypted = aes.decrypt(encrypted, key, iv);

        assertFalse(Arrays.equals(original, encrypted));
        assertArrayEquals(original, decrypted);
    }

    @Test
    void aesDebeRechazarDescifradoConClaveIncorrecta() {
        AesEncryptionService aes = new AesEncryptionService();
        byte[] iv = new byte[12];
        SecretKey key = new SecretKeySpec(new byte[32], "AES");
        SecretKey wrong = new SecretKeySpec(new byte[]{1,2,3,4,5,6,7,8,9,10,11,12,13,14,15,16,17,18,19,20,21,22,23,24,25,26,27,28,29,30,31,32}, "AES");
        byte[] encrypted = aes.encrypt(new byte[]{1, 2, 3}, key, iv);

        assertThrows(RuntimeException.class, () -> aes.decrypt(encrypted, wrong, iv));
    }

    @Test
    void keyDerivationDebeGenerarClavesDeterministasYSepararLegacy() {
        KeyDerivationService service = new KeyDerivationService();
        byte[] salt = new byte[16];

        SecretKey key1 = service.deriveKey("password1", salt);
        SecretKey key2 = service.deriveKey("password1", salt);
        SecretKey legacy = service.deriveLegacyKey("password1", salt);

        assertArrayEquals(key1.getEncoded(), key2.getEncoded());
        assertFalse(Arrays.equals(key1.getEncoded(), legacy.getEncoded()));
        assertEquals(32, key1.getEncoded().length);
    }

    @Test
    void keyDerivationDebeValidarPasswordYSalt() {
        KeyDerivationService service = new KeyDerivationService();

        assertThrows(IllegalArgumentException.class, () -> service.deriveKey(null, new byte[]{1}));
        assertThrows(IllegalArgumentException.class, () -> service.deriveKey("   ", new byte[]{1}));
        assertThrows(IllegalArgumentException.class, () -> service.deriveKey("password1", null));
        assertThrows(IllegalArgumentException.class, () -> service.deriveKey("password1", new byte[0]));
    }

    @Test
    void cryptoUtilsDebeGenerarBytesDelTamanoSolicitado() {
        byte[] bytes = CryptoUtils.randomBytes(32);
        assertEquals(32, bytes.length);
    }

    @Test
    void usuarioAutenticadoDebeAplicarPermisosPorRol() {
        AuthenticatedUser admin = new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR);
        AuthenticatedUser user = new AuthenticatedUser("juan", "Juan", UserRole.USUARIO);

        assertEquals("admin", admin.getUsername());
        assertEquals("Administrador", admin.getDisplayName());
        assertTrue(admin.isAdmin());
        assertTrue(admin.hasPermission(UserPermission.VER_AUDITORIA));
        assertEquals(UserPermission.values().length, AuthenticatedUser.permissionsFor(UserRole.ADMINISTRADOR).size());

        assertFalse(user.isAdmin());
        assertTrue(user.hasPermission(UserPermission.VER_INICIO));
        assertTrue(user.hasPermission(UserPermission.VER_FINANZAS));
        assertFalse(user.hasPermission(UserPermission.VER_AUDITORIA));
        assertEquals(4, AuthenticatedUser.permissionsFor(UserRole.USUARIO).size());
    }

    @Test
    void rolesYEstadosDebenExponerSusDescripciones() {
        assertEquals("Administrador", UserRole.ADMINISTRADOR.getDisplayName());
        assertEquals("Usuario", UserRole.USUARIO.getDisplayName());
        assertEquals("Administrador", UserRole.ADMINISTRADOR.getDisplayName());
    }
}
