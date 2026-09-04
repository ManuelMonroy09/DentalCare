package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.financiero.Pago;
import mx.dentalcare.infrastructure.persistence.file.PagoData;
import mx.dentalcare.repository.PagoRepository;
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
public class EncryptedFilePagoRepository implements PagoRepository {
    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;
    private final Path filePath = Path.of("data", "pagos.dat");

    public EncryptedFilePagoRepository(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override
    public Pago save(Pago pago) {
        PagoData data = loadData();
        if (pago.getId() == null) {
            long id = 1;
            boolean exists;
            do {
                final long candidate = id;
                exists = data.getPagos().stream().anyMatch(p -> p.getId() != null && candidate == p.getId());
                if (exists) id++;
            } while (exists);
            pago.setId(id);
        }
        data.getPagos().removeIf(p -> pago.getId().equals(p.getId()));
        data.getPagos().add(pago);
        saveData(data);
        return pago;
    }

    @Override
    public List<Pago> findAll() {
        return new ArrayList<>(loadData().getPagos());
    }

    @Override
    public Optional<Pago> findById(Long id) {
        if (id == null) return Optional.empty();
        return loadData().getPagos().stream()
                .filter(p -> id.equals(p.getId()))
                .findFirst();
    }

    private PagoData loadData() {
        SecretKey key = securitySession.requireMasterKey();
        PagoData data = storage.load(filePath, PagoData.class, key);
        if (data == null) data = new PagoData();
        if (data.getPagos() == null) data.setPagos(new ArrayList<>());
        return data;
    }

    private void saveData(PagoData data) {
        storage.save(filePath, data, securitySession.requireMasterKey());
    }
}
