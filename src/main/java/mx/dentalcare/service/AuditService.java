package mx.dentalcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.domain.auditoria.AuditEntry;
import mx.dentalcare.infrastructure.persistence.file.AuditData;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.EncryptedFileStorage;
import mx.dentalcare.security.KeyDerivationService;
import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.SecuritySession;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class AuditService {

    private static final Path AUDIT_FILE = DataDirectoryService.resolve("auditoria.dat");

    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;

    public AuditService(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    public synchronized void registrar(String modulo, String accion, String entidad, Long entidadId,
                                       String descripcion, String valorAnterior, String valorNuevo,
                                       String resultado) {
        AuthenticatedUser user = securitySession.requireCurrentUser();
        AuditEntry entry = new AuditEntry();
        entry.setId(siguienteId());
        entry.setFechaHora(LocalDateTime.now());
        entry.setUsername(user.getUsername());
        entry.setDisplayName(user.getDisplayName());
        entry.setRole(user.getRole().name());
        entry.setModulo(valor(modulo));
        entry.setAccion(valor(accion));
        entry.setEntidad(valor(entidad));
        entry.setEntidadId(entidadId);
        entry.setDescripcion(valor(descripcion));
        entry.setValorAnterior(valor(valorAnterior));
        entry.setValorNuevo(valor(valorNuevo));
        entry.setResultado(valor(resultado));

        AuditData data = cargar();
        data.getEntries().add(entry);
        guardar(data);
    }

    public synchronized List<AuditEntry> obtenerTodos() {
        requireAdmin();
        return cargar().getEntries().stream()
                .sorted(Comparator.comparing(AuditEntry::getFechaHora, Comparator.nullsLast(Comparator.reverseOrder())))
                .toList();
    }

    private AuditData cargar() {
        SecretKey masterKey = securitySession.requireMasterKey();
        AuditData data = storage.load(AUDIT_FILE, AuditData.class, masterKey);
        if (data == null) data = new AuditData();
        if (data.getEntries() == null) data.setEntries(new ArrayList<>());
        return data;
    }

    private void guardar(AuditData data) {
        storage.save(AUDIT_FILE, data, securitySession.requireMasterKey());
    }

    private long siguienteId() {
        return cargar().getEntries().stream()
                .map(AuditEntry::getId)
                .filter(id -> id != null)
                .max(Long::compareTo)
                .orElse(0L) + 1L;
    }

    private void requireAdmin() {
        if (!securitySession.requireCurrentUser().isAdmin()) {
            throw new SecurityException("No tienes privilegios de administrador para consultar la auditoría.");
        }
    }

    private String valor(String value) {
        return value == null || value.isBlank() ? "-" : value.trim();
    }
}
