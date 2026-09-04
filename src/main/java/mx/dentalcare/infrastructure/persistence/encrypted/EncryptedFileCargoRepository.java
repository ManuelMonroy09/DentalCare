package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.financiero.Cargo;
import mx.dentalcare.infrastructure.persistence.file.CargoData;
import mx.dentalcare.repository.CargoRepository;
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
public class EncryptedFileCargoRepository implements CargoRepository {
    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;
    private final Path filePath = Path.of("data", "cargos.dat");

    public EncryptedFileCargoRepository(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override public Cargo save(Cargo cargo) {
        CargoData data = loadData();
        if (cargo.getId() == null) {
            long id = 1;
            while (data.getCargos().stream().anyMatch(c -> id == c.getId())) id++;
            cargo.setId(id);
        }
        data.getCargos().removeIf(c -> cargo.getId().equals(c.getId()));
        data.getCargos().add(cargo);
        saveData(data);
        return cargo;
    }

    @Override public List<Cargo> findAll() { return new ArrayList<>(loadData().getCargos()); }

    @Override public Optional<Cargo> findById(Long id) {
        if (id == null) return Optional.empty();
        return loadData().getCargos().stream().filter(c -> id.equals(c.getId())).findFirst();
    }

    @Override public Optional<Cargo> findByCitaId(Long citaId) {
        if (citaId == null) return Optional.empty();
        return loadData().getCargos().stream().filter(c -> citaId.equals(c.getCitaId())).findFirst();
    }

    private CargoData loadData() {
        SecretKey key = securitySession.requireMasterKey();
        CargoData data = storage.load(filePath, CargoData.class, key);
        if (data == null) data = new CargoData();
        if (data.getCargos() == null) data.setCargos(new ArrayList<>());
        return data;
    }

    private void saveData(CargoData data) { storage.save(filePath, data, securitySession.requireMasterKey()); }
}
