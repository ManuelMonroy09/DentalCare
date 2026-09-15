package mx.dentalcare.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
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

    public UserService(
            ObjectMapper objectMapper,
            KeyDerivationService keyDerivationService,
            AesEncryptionService aesEncryptionService,
            MasterKeyService masterKeyService,
            SecuritySession securitySession
    ) {
        this.objectMapper = objectMapper;
        this.keyDerivationService = keyDerivationService;
        this.aesEncryptionService = aesEncryptionService;
        this.masterKeyService = masterKeyService;
        this.securitySession = securitySession;
    }

    public synchronized void initializeAdmin(String password) {
        if (!Files.exists(USERS_FILE)) {
            SecretKey masterKey = securitySession.requireMasterKey();
            UserStore store = new UserStore();
            store.users = new ArrayList<>();
            store.users.add(createRecord("admin", "Administrador", UserRole.ADMINISTRADOR, password, masterKey));
            saveStore(store);
        }

        securitySession.setCurrentUser(new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR));
    }

    public synchronized AuthenticatedUser authenticate(String username, String password) {
        validateCredentials(username, password);

        if (!Files.exists(USERS_FILE)) {
            if (!"admin".equalsIgnoreCase(username)) {
                throw new SecurityException("Usuario o contraseña incorrectos.");
            }

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
            return user;
        } catch (SecurityException e) {
            securitySession.clear();
            throw e;
        } catch (Exception e) {
            securitySession.clear();
            throw new SecurityException("Usuario o contraseña incorrectos.", e);
        }
    }

    public synchronized List<StoredUser> listUsers() {
        requireAdmin();
        return new ArrayList<>(loadStore().users);
    }

    public synchronized void createUser(String username, String displayName, String password, UserRole role) {
        requireAdmin();
        validateCredentials(username, password);
        if (displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("El nombre del usuario es obligatorio.");
        }
        if (role == null) {
            throw new IllegalArgumentException("El rol del usuario es obligatorio.");
        }

        UserStore store = loadStore();
        if (findRecord(store, username) != null) {
            throw new IllegalArgumentException("Ya existe un usuario con ese nombre.");
        }

        SecretKey masterKey = securitySession.requireMasterKey();
        store.users.add(createRecord(username.trim(), displayName.trim(), role, password, masterKey));
        saveStore(store);
    }

    public synchronized void setUserActive(String username, boolean active) {
        requireAdmin();
        UserStore store = loadStore();
        StoredUser record = findRecord(store, username);
        if (record == null) {
            throw new IllegalArgumentException("No se encontró el usuario.");
        }
        if (record.role == UserRole.ADMINISTRADOR && !active) {
            throw new IllegalArgumentException("El administrador principal no puede desactivarse.");
        }
        record.active = active;
        saveStore(store);
    }

    public synchronized void changeCurrentUserPassword(String currentPassword, String newPassword) {
        validatePassword(newPassword);
        AuthenticatedUser current = securitySession.requireCurrentUser();
        authenticate(current.getUsername(), currentPassword);

        UserStore store = loadStore();
        StoredUser record = findRecord(store, current.getUsername());
        if (record == null) {
            throw new IllegalStateException("No se encontró el usuario autenticado.");
        }

        SecretKey masterKey = securitySession.requireMasterKey();
        updateCredentials(record, newPassword, masterKey);
        saveStore(store);
        securitySession.setCurrentUser(new AuthenticatedUser(record.username, record.displayName, record.role));
    }

    public AuthenticatedUser currentUser() {
        return securitySession.requireCurrentUser();
    }

    private StoredUser createRecord(String username, String displayName, UserRole role, String password, SecretKey masterKey) {
        StoredUser record = new StoredUser();
        record.username = username.trim();
        record.displayName = displayName.trim();
        record.role = role;
        record.active = true;
        record.createdAt = LocalDateTime.now().toString();
        updateCredentials(record, password, masterKey);
        return record;
    }

    private void updateCredentials(StoredUser record, String password, SecretKey masterKey) {
        validatePassword(password);
        byte[] passwordSalt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
        byte[] keySalt = CryptoUtils.randomBytes(EncryptionConstants.SALT_LENGTH);
        byte[] keyIv = CryptoUtils.randomBytes(EncryptionConstants.IV_LENGTH);

        SecretKey hashKey = keyDerivationService.deriveKey(password, passwordSalt);
        SecretKey protectionKey = keyDerivationService.deriveKey(password, keySalt);
        byte[] wrapped = aesEncryptionService.encrypt(masterKey.getEncoded(), protectionKey, keyIv);

        record.passwordSalt = Base64.getEncoder().encodeToString(passwordSalt);
        record.passwordHash = Base64.getEncoder().encodeToString(hashKey.getEncoded());
        record.keySalt = Base64.getEncoder().encodeToString(keySalt);
        record.keyIv = Base64.getEncoder().encodeToString(keyIv);
        record.wrappedMasterKey = Base64.getEncoder().encodeToString(wrapped);
    }

    private boolean verifyPassword(String password, String expectedHash, String salt) {
        try {
            SecretKey key = keyDerivationService.deriveKey(password, Base64.getDecoder().decode(salt));
            return MessageDigest.isEqual(key.getEncoded(), Base64.getDecoder().decode(expectedHash));
        } catch (Exception e) {
            return false;
        }
    }

    private StoredUser findRecord(UserStore store, String username) {
        return store.users.stream()
                .filter(user -> user.username != null && user.username.equalsIgnoreCase(username.trim()))
                .findFirst()
                .orElse(null);
    }

    private void requireAdmin() {
        if (!securitySession.requireCurrentUser().isAdmin()) {
            throw new SecurityException("No tienes privilegios de administrador para realizar esta operación.");
        }
    }

    private UserStore loadStore() {
        try {
            if (!Files.exists(USERS_FILE)) {
                UserStore store = new UserStore();
                store.users = new ArrayList<>();
                return store;
            }
            UserStore store = objectMapper.readValue(Files.readString(USERS_FILE), UserStore.class);
            if (store.users == null) {
                store.users = new ArrayList<>();
            }
            return store;
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible leer la configuración de usuarios.", e);
        }
    }

    private void saveStore(UserStore store) {
        try {
            Files.createDirectories(USERS_FILE.getParent());
            Path temporary = USERS_FILE.resolveSibling("users.json.tmp");
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(temporary.toFile(), store);
            try {
                Files.move(temporary, USERS_FILE, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (java.nio.file.AtomicMoveNotSupportedException e) {
                Files.move(temporary, USERS_FILE, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception e) {
            throw new IllegalStateException("No fue posible guardar la configuración de usuarios.", e);
        }
    }

    private void validateCredentials(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("El usuario es obligatorio.");
        }
        validatePassword(password);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("La contraseña debe tener al menos 8 caracteres.");
        }
    }

    public static class UserStore {
        public List<StoredUser> users = new ArrayList<>();
    }

    public static class StoredUser {
        public String username;
        public String displayName;
        public UserRole role;
        public boolean active;
        public String createdAt;
        public String passwordSalt;
        public String passwordHash;
        public String keySalt;
        public String keyIv;
        public String wrappedMasterKey;
    }
}
