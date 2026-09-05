package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.infrastructure.persistence.file.CitaData;
import mx.dentalcare.repository.CitaRepository;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.EncryptedFileStorage;
import mx.dentalcare.security.KeyDerivationService;
import mx.dentalcare.security.SecuritySession;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import javax.crypto.SecretKey;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class EncryptedFileCitaRepository implements CitaRepository {
    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;
    private final Path filePath = DataDirectoryService.resolve("citas.dat");

    public EncryptedFileCitaRepository(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override public Cita save(Cita cita) {
        CitaData data = loadData();
        if (cita.getId() == null) {
            Long nuevoId = 1L;
            boolean idExiste;
            do {
                final Long idBuscado = nuevoId;
                idExiste = data.getCitas().stream().anyMatch(c -> c.getId() != null && c.getId().equals(idBuscado));
                if (idExiste) nuevoId++;
            } while (idExiste);
            cita.setId(nuevoId);
        }
        data.getCitas().removeIf(c -> c.getId() != null && c.getId().equals(cita.getId()));
        data.getCitas().add(cita);
        saveData(data);
        return cita;
    }

    @Override public Optional<Cita> findById(Long id) {
        if (id == null) return Optional.empty();
        return loadData().getCitas().stream().filter(c -> c.getId() != null && c.getId().equals(id)).findFirst();
    }

    @Override public List<Cita> findAll() { return new ArrayList<>(loadData().getCitas()); }

    @Override public void deleteById(Long id) {
        if (id == null) return;
        CitaData data = loadData();
        data.getCitas().removeIf(c -> c.getId() != null && c.getId().equals(id));
        saveData(data);
    }

    private CitaData loadData() {
        SecretKey masterKey = securitySession.requireMasterKey();
        CitaData data = storage.load(filePath, CitaData.class, masterKey);
        if (data == null) return new CitaData();
        if (data.getCitas() == null) data.setCitas(new ArrayList<>());
        return data;
    }

    private void saveData(CitaData data) { storage.save(filePath, data, securitySession.requireMasterKey()); }
}
