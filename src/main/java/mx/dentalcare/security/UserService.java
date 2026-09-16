package mx.dentalcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.service.AuditService;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@Service
public class UserService {

    private static final Path USERS_FILE = DataDirectoryService.resolve("users.json");

    private final ObjectMapper objectMapper;
    private final KeyDerivationService keyDerivationService;
    private final AesEncryptionService aesEncryptionService;
    private final MasterKeyService masterKeyService;
    private final SecuritySession securitySession;
    private final AuditService auditService;

    public UserService(ObjectMapper objectMapper, KeyDerivationService keyDerivationService,
                       AesEncryptionService aesEncryptionService, MasterKeyService masterKeyService,
                       SecuritySession securitySession, AuditService auditService) {
        this.objectMapper = objectMapper;
        this.keyDerivationService = keyDerivationService;
        this.aesEncryptionService = aesEncryptionService;
        this.masterKeyService = masterKeyService;
        this.securitySession = securitySession;
        this.auditService = auditService;
    }

    public synchronized void initializeAdmin(String password) { initializeAdmin(password, null); }

    public synchronized void initializeAdmin(String password, String recoveryKey) {
        if (!Files.exists(USERS_FILE)) {
            SecretKey masterKey = securitySession.requireMasterKey();
            UserStore store = new UserStore();
            store.users = new ArrayList<>();
            StoredUser admin = createRecord("admin", "Administrador", UserRole.ADMINISTRADOR, password, masterKey);
            if (recoveryKey != null && !recoveryKey.isBlank()) configureRecovery(admin, recoveryKey, masterKey);
            store.users.add(admin);
            saveStore(store);
        }
        securitySession.setCurrentUser(new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR));
        auditService.registrar("SEGURIDAD", "INICIALIZAR_ADMIN", "USUARIO", null,
                "Cuenta administrativa inicializada", null, "admin", "EXITOSO");
    }

    public synchronized AuthenticatedUser authenticate(String username, String password) {
        validateCredentials(username, password);
        if (!Files.exists(USERS_FILE)) {
            if (!"admin".equalsIgnoreCase(username)) throw new SecurityException("Usuario o contraseña incorrectos.");
            masterKeyService.unlock(password);
            initializeAdmin(password);
            return securitySession.requireCurrentUser();
        }
        UserStore store = loadStore();
        StoredUser record = findRecord(store, username);
        if (record == null || !record.active || !verifyPassword(password, record.passwordHash, record.passwordSalt)) {
            throw new SecurityException("Usuario o contraseña incorrectos.");
        }
        try {
            byte[] salt = Base64.getDecoder().decode(record.keySalt);
            byte[] iv = Base64.getDecoder().decode(record.keyIv);
            byte[] wrappedMasterKey = Base64.getDecoder().decode(record.wrappedMasterKey);
            SecretKey protectionKey = keyDerivationService.deriveKey(password, salt);
            byte[] masterKeyBytes = aesEncryptionService.decrypt(wrappedMasterKey, protectionKey, iv);
            if (masterKeyBytes.length != EncryptionConstants.MASTER_KEY_LENGTH_BYTES) {
                throw new SecurityException("La clave de seguridad del usuario no es válida.");
            }
            SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
            securitySession.authenticate(masterKey);
            AuthenticatedUser user = new AuthenticatedUser(record.username, record.displayName, record.role);
            securitySession.setCurrentUser(user);
            auditService.registrar("SEGURIDAD", "INICIO_SESION", "USUARIO", null,
                    "Inicio de sesión correcto", null, record.username, "EXITOSO");
            return user;
        } catch (SecurityException e) {
            securitySession.clear();
            throw e;
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("Usuario o contraseña incorrectos.", e);
        }
    }

    public synchronized boolean hasRecoveryKey(String username) {
        StoredUser record = findRecord(loadStore(), username);
        return record != null && record.recoverySalt != null && record.recoveryIv != null
                && record.recoveryWrappedMasterKey != null;
    }

    public synchronized String generateRecoveryKey(String username) {
        AuthenticatedUser current = securitySession.requireCurrentUser();
        if (!current.getUsername().equalsIgnoreCase(username)) {
            throw new SecurityException("No puedes generar una clave de recuperación para otra cuenta.");
        }
        UserStore store = loadStore();
        StoredUser record = findRecord(store, username);
        if (record == null || !record.active) throw new SecurityException("No se encontró la cuenta de usuario.");
        if (hasRecoveryKey(username)) throw new IllegalStateException("La cuenta ya tiene una clave de recuperación.");
        SecretKey masterKey = securitySession.requireMasterKey();
        String recoveryKey = generateRecoveryKeyValue();
        configureRecovery(record, recoveryKey, masterKey);
        saveStore(store);
        auditService.registrar("SEGURIDAD", "GENERAR_CLAVE_RECUPERACION", "USUARIO", null,
                "Clave de recuperación generada para la cuenta", null, record.username, "EXITOSO");
        return recoveryKey;
    }

    public synchronized String recoverPassword(String username, String recoveryKey, String newPassword) {
        validatePassword(newPassword);
        UserStore store = loadStore();
        StoredUser record = findRecord(store, username);
        if (record == null || !record.active) throw new SecurityException("No se encontró la cuenta de usuario.");
        if (!hasRecoveryKey(username)) throw new SecurityException("La cuenta no tiene una clave de recuperación configurada.");
        try {
            byte[] salt = Base64.getDecoder().decode(record.recoverySalt);
            byte[] iv = Base64.getDecoder().decode(record.recoveryIv);
            byte[] wrappedMasterKey = Base64.getDecoder().decode(record.recoveryWrappedMasterKey);
            SecretKey protectionKey = keyDerivationService.deriveKey(normalizeRecoveryKey(recoveryKey), salt);
            byte[] masterKeyBytes = aesEncryptionService.decrypt(wrappedMasterKey, protectionKey, iv);
            if (masterKeyBytes.length != EncryptionConstants.MASTER_KEY_LENGTH_BYTES) {
                throw new SecurityException("La clave de recuperación no es válida.");
            }
            SecretKey masterKey = new SecretKeySpec(masterKeyBytes, "AES");
            securitySession.authenticate(masterKey);
            updateCredentials(record, newPassword, masterKey);
            String newRecoveryKey = generateRecoveryKeyValue();
            configureRecovery(record, newRecoveryKey, masterKey);
            saveStore(store);
            AuthenticatedUser user = new AuthenticatedUser(record.username, record.displayName, record.role);
            securitySession.setCurrentUser(user);
            auditService.registrar("SEGURIDAD", "RECUPERAR_CONTRASENA", "USUARIO", null,
                    "Contraseña restablecida mediante clave de recuperación", null, record.username, "EXITOSO");
            return newRecoveryKey;
        } catch (SecurityException e) {
            securitySession.clear();
            throw e;
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("La clave de recuperación no es correcta.", e);
        }
    }

    public synchronized List<StoredUser> listUsers() { requireAdmin(); return new ArrayList<>(loadStore().users); }

    public synchronized void createUser(String username, String displayName, String password, UserRole role) {
        requireAdmin();
        validateCredentials(username, password);
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
        if (role == null) throw new IllegalArgumentException("El rol del usuario es obligatorio.");
        UserStore store = loadStore();
        if (findRecord(store, username) != null) throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        SecretKey masterKey = securitySession.requireMasterKey();
        store.users.add(createRecord(username.trim(), displayName.trim(), role, password, masterKey));
        saveStore(store);
        auditService.registrar("SEGURIDAD", "CREAR_USUARIO", "USUARIO", null,
                "Usuario creado", null, username.trim() + " | " + role.name(), "EXITOSO");
    }

    public synchronized void updateDisplayName(String username, String displayName) {
        requireAdmin();
        if (displayName == null || displayName.isBlank()) throw new IllegalArgumentException("El nombre visible es obligatorio.");
        UserStore store = loadStore(); StoredUser record = findRecord(store, username);
        if (record == null) throw new IllegalArgumentException("No se encontró el usuario.");
        String anterior = record.displayName; record.displayName = displayName.trim(); saveStore(store);
        AuthenticatedUser current = securitySession.requireCurrentUser();
        if (current.getUsername().equalsIgnoreCase(record.username)) securitySession.setCurrentUser(new AuthenticatedUser(record.username, record.displayName, record.role));
        auditService.registrar("SEGURIDAD", "ACTUALIZAR_USUARIO", "USUARIO", null, "Nombre visible actualizado", anterior, record.displayName, "EXITOSO");
    }

    public synchronized void setUserActive(String username, boolean active) {
        requireAdmin(); UserStore store = loadStore(); StoredUser record = findRecord(store, username);
        if (record == null) throw new IllegalArgumentException("No se encontró el usuario.");
        if (record.username.equalsIgnoreCase(securitySession.requireCurrentUser().getUsername()) && !active) throw new IllegalArgumentException("No puedes desactivar el usuario con el que has iniciado sesión.");
        boolean anterior = record.active; record.active = active; saveStore(store);
        auditService.registrar("SEGURIDAD", active ? "ACTIVAR_USUARIO" : "DESACTIVAR_USUARIO", "USUARIO", null, active ? "Usuario activado" : "Usuario desactivado", String.valueOf(anterior), String.valueOf(active), "EXITOSO");
    }

    public synchronized void changeCurrentUserPassword(String currentPassword, String newPassword) {
        validatePassword(newPassword); AuthenticatedUser current = securitySession.requireCurrentUser(); authenticate(current.getUsername(), currentPassword);
        UserStore store = loadStore(); StoredUser record = findRecord(store, current.getUsername());
        if (record == null) throw new IllegalStateException("No se encontró el usuario autenticado.");
        SecretKey masterKey = securitySession.requireMasterKey(); updateCredentials(record, newPassword, masterKey); saveStore(store);
        securitySession.setCurrentUser(new AuthenticatedUser(record.username, record.displayName, record.role));
        auditService.registrar("SEGURIDAD", "CAMBIAR_CONTRASENA", "USUARIO", null, "Contraseña actualizada", null, record.username, "EXITOSO");
    }

    public synchronized AuthenticatedUser resetAdminPassword(String newPassword) {
        validatePassword(newPassword); UserStore store = loadStore();
        StoredUser admin = store.users.stream().filter(user -> user.active && user.role == UserRole.ADMINISTRADOR).findFirst()
                .orElseThrow(() -> new IllegalStateException("No se encontró una cuenta de administrador activa."));
        SecretKey masterKey = securitySession.requireMasterKey(); updateCredentials(admin, newPassword, masterKey); saveStore(store);
        AuthenticatedUser user = new AuthenticatedUser(admin.username, admin.displayName, admin.role); securitySession.setCurrentUser(user);
        auditService.registrar("SEGURIDAD", "RECUPERAR_CONTRASENA", "USUARIO", null, "Contraseña del administrador restablecida mediante recuperación", null, admin.username, "EXITOSO");
        return user;
    }

    public AuthenticatedUser currentUser() { return securitySession.requireCurrentUser(); }

    private StoredUser createRecord(String username, String displayName, UserRole role, String password, SecretKey masterKey) {
        StoredUser record = new StoredUser(); record.username = username.trim(); record.displayName = displayName.trim(); record.role = role; record.active = true; record.createdAt = LocalDateTime.now().toString(); updateCredentials(record, password, masterKey); return record;
    }

    private void updateCredentials(StoredUser record, String password, SecretKey masterKey) {
        validatePassword(password); byte[] passwordSalt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH); byte[] keySalt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH); byte[] keyIv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);
        SecretKey hashKey = keyDerivationService.deriveKey(password, passwordSalt); SecretKey protectionKey = keyDerivationService.deriveKey(password, keySalt); byte[] wrapped = aesEncryptionService.encrypt(masterKey.getEncoded(), protectionKey, keyIv);
        record.passwordSalt = Base64.getEncoder().encodeToString(passwordSalt); record.passwordHash = Base64.getEncoder().encodeToString(hashKey.getEncoded()); record.keySalt = Base64.getEncoder().encodeToString(keySalt); record.keyIv = Base64.getEncoder().encodeToString(keyIv); record.wrappedMasterKey = Base64.getEncoder().encodeToString(wrapped);
    }

    private void configureRecovery(StoredUser record, String recoveryKey, SecretKey masterKey) {
        byte[] salt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH); byte[] iv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH); SecretKey protectionKey = keyDerivationService.deriveKey(normalizeRecoveryKey(recoveryKey), salt); byte[] wrapped = aesEncryptionService.encrypt(masterKey.getEncoded(), protectionKey, iv);
        record.recoverySalt = Base64.getEncoder().encodeToString(salt); record.recoveryIv = Base64.getEncoder().encodeToString(iv); record.recoveryWrappedMasterKey = Base64.getEncoder().encodeToString(wrapped);
    }

    private String generateRecoveryKeyValue() { return formatRecoveryKey(Base64.getUrlEncoder().withoutPadding().encodeToString(CryptoUtils.randomBytes(EncryptionConstants.MASTER_KEY_LENGTH_BYTES))); }

    private String normalizeRecoveryKey(String recoveryKey) {
        if (recoveryKey == null) throw new SecurityException("La clave de recuperación no es correcta.");
        String value = recoveryKey.replaceAll("\\s", "");
        if (value.length() == 48) {
            int[] positions = {8, 17, 26, 35, 44}; StringBuilder normalized = new StringBuilder(43);
            for (int i = 0; i < value.length(); i++) { boolean separator = false; for (int position : positions) if (i == position) { separator = true; break; } if (!separator) normalized.append(value.charAt(i)); }
            value = normalized.toString();
        }
        return value;
    }

    private String formatRecoveryKey(String recoveryKey) {
        StringBuilder formatted = new StringBuilder(); for (int i = 0; i < recoveryKey.length(); i++) { if (i > 0 && i % 8 == 0) formatted.append('-'); formatted.append(recoveryKey.charAt(i)); } return formatted.toString();
    }

    private boolean verifyPassword(String password, String expectedHash, String salt) {
        try { SecretKey key = keyDerivationService.deriveKey(password, Base64.getDecoder().decode(salt)); return MessageDigest.isEqual(key.getEncoded(), Base64.getDecoder().decode(expectedHash)); } catch (Exception e) { return false; }
    }

    private StoredUser findRecord(UserStore store, String username) { return store.users.stream().filter(user -> user.username != null && user.username.equalsIgnoreCase(username.trim())).findFirst().orElse(null); }

    private void requireAdmin() { if (!securitySession.requireCurrentUser().isAdmin()) throw new SecurityException("No tienes privilegios de administrador para realizar esta operación."); }

    private UserStore loadStore() {
        try { if (!Files.exists(USERS_FILE)) { UserStore store = new UserStore(); store.users = new ArrayList<>(); return store; } UserStore store = objectMapper.readValue(Files.readString(USERS_FILE), UserStore.class); if (store.users == null) store.users = new ArrayList<>(); return store; }
        catch (Exception e) { throw new IllegalStateException("No fue posible leer la configuración de usuarios.", e); }
    }

    private void saveStore(UserStore store) {
        try { Files.createDirectories(USERS_FILE.getParent()); Path temporary = USERS_FILE.resolveSibling("users.json.tmp"); objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), store); try { Files.move(temporary, USERS_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); } catch (java.nio.file.AtomicMoveNotSupportedException e) { Files.move(temporary, USERS_FILE, StandardCopyOption.REPLACE_EXISTING); } }
        catch (Exception e) { throw new IllegalStateException("No fue posible guardar la configuración de usuarios.", e); }
    }

    private void validateCredentials(String username, String password) { if (username == null || username.isBlank()) throw new IllegalArgumentException("El usuario es obligatorio."); validatePassword(password); }
    private void validatePassword(String password) { if (password == null || password.length() < 8) throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres."); }

    public static class UserStore { public List<StoredUser> users = new ArrayList<>(); }
    public static class StoredUser {
        public String username; public String displayName; public UserRole role; public boolean active; public String createdAt;
        public String passwordSalt; public String passwordHash; public String keySalt; public String keyIv; public String wrappedMasterKey;
        public String recoverySalt; public String recoveryIv; public String recoveryWrappedMasterKey;
    }
}
