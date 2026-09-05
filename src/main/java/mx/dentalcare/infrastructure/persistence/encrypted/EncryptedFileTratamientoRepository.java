package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.infrastructure.persistence.file.TratamientoData;
import mx.dentalcare.repository.TratamientoRepository;
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
public class EncryptedFileTratamientoRepository implements TratamientoRepository {
    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;
    private final Path filePath = DataDirectoryService.resolve("tratamientos.dat");

    public EncryptedFileTratamientoRepository(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override public Tratamiento save(Tratamiento tratamiento) {
        TratamientoData data = loadData();
        if (tratamiento.getId() == null) {
            Long nuevoId = 1L;
            boolean idExiste;
            do {
                final Long idBuscado = nuevoId;
                idExiste = data.getTratamientos().stream().anyMatch(t -> t.getId() != null && t.getId().equals(idBuscado));
                if (idExiste) nuevoId++;
            } while (idExiste);
            tratamiento.setId(nuevoId);
        }
        data.getTratamientos().removeIf(t -> t.getId() != null && t.getId().equals(tratamiento.getId()));
        data.getTratamientos().add(tratamiento);
        saveData(data);
        return tratamiento;
    }

    @Override public Optional<Tratamiento> findById(Long id) {
        if (id == null) return Optional.empty();
        return loadData().getTratamientos().stream().filter(t -> t.getId() != null && t.getId().equals(id)).findFirst();
    }

    @Override public List<Tratamiento> findAll() { return new ArrayList<>(loadData().getTratamientos()); }

    @Override public void deleteById(Long id) {
        if (id == null) return;
        TratamientoData data = loadData();
        data.getTratamientos().removeIf(t -> t.getId() != null && t.getId().equals(id));
        saveData(data);
    }

    private TratamientoData loadData() {
        SecretKey masterKey = securitySession.requireMasterKey();
        TratamientoData data = storage.load(filePath, TratamientoData.class, masterKey);
        if (data == null) return new TratamientoData();
        if (data.getTratamientos() == null) data.setTratamientos(new ArrayList<>());
        return data;
    }

    private void saveData(TratamientoData data) { storage.save(filePath, data, securitySession.requireMasterKey()); }
}
