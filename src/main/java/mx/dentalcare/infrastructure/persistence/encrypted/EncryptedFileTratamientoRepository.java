package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.infrastructure.persistence.file.TratamientoData;
import mx.dentalcare.repository.TratamientoRepository;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.EncryptedFileStorage;
import mx.dentalcare.security.KeyDerivationService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class EncryptedFileTratamientoRepository implements TratamientoRepository {
    private static final String PASSWORD = "TemporalDentalCare2026";
    private final EncryptedFileStorage storage;
    private final Path filePath = Path.of("data", "tratamientos.dat");

    public EncryptedFileTratamientoRepository(
            ObjectMapper objectMapper
    ) {
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override
    public Tratamiento save(Tratamiento tratamiento) {

        TratamientoData data = loadData();
        if (tratamiento.getId() == null) {
            Long nuevoId = 1L;
            boolean idExiste;
            do {
                idExiste = false;
                for (Tratamiento t : data.getTratamientos()) {
                    if (t.getId() != null && t.getId().equals(nuevoId)) {
                        idExiste = true;
                        break;
                    }
                }

                if (idExiste) {
                    nuevoId++;
                }

            } while (idExiste);
            tratamiento.setId(nuevoId);
        }

        data.getTratamientos().removeIf(t -> t.getId() != null && t.getId().equals(tratamiento.getId()));
        data.getTratamientos().add(tratamiento);
        saveData(data);
        return tratamiento;
    }

    @Override
    public Optional<Tratamiento> findById(Long id) {
        return loadData().getTratamientos().stream().filter(t -> t.getId().equals(id)).findFirst();
    }

    @Override
    public List<Tratamiento> findAll() {
        return new ArrayList<>(loadData().getTratamientos());
    }

    @Override
    public void deleteById(Long id) {
        TratamientoData data = loadData();
        data.getTratamientos().removeIf(t -> t.getId().equals(id));
        saveData(data);
    }

    private TratamientoData loadData() {
        TratamientoData data = storage.load(filePath, TratamientoData.class, PASSWORD);
        if (data == null) {
            return new TratamientoData();
        }
        return data;
    }

    private void saveData(TratamientoData data) {
        storage.save(filePath, data, PASSWORD);
    }
}