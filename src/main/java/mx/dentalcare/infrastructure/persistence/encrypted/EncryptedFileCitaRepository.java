package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.cita.Cita;
import mx.dentalcare.infrastructure.persistence.file.CitaData;
import mx.dentalcare.repository.CitaRepository;
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
public class EncryptedFileCitaRepository implements CitaRepository {

    private static final String PASSWORD = "TemporalDentalCare2026";

    private final EncryptedFileStorage storage;

    private final Path filePath = Path.of("data", "citas.dat");

    public EncryptedFileCitaRepository(ObjectMapper objectMapper) {

        this.storage = new EncryptedFileStorage(
                objectMapper,
                new KeyDerivationService(),
                new AesEncryptionService()
        );
    }

    @Override
    public Cita save(Cita cita) {

        CitaData data = loadData();

        if (cita.getId() == null) {

            Long nuevoId = 1L;
            boolean idExiste;

            do {

                idExiste = false;

                for (Cita existente : data.getCitas()) {

                    if (existente.getId() != null
                            && existente.getId().equals(nuevoId)) {

                        idExiste = true;
                        break;
                    }
                }

                if (idExiste) {
                    nuevoId++;
                }

            } while (idExiste);

            cita.setId(nuevoId);
        }

        data.getCitas().removeIf(
                existente ->
                        existente.getId() != null
                                && existente.getId().equals(cita.getId())
        );

        data.getCitas().add(cita);

        saveData(data);

        return cita;
    }

    @Override
    public Optional<Cita> findById(Long id) {

        if (id == null) {
            return Optional.empty();
        }

        return loadData()
                .getCitas()
                .stream()
                .filter(cita ->
                        cita.getId() != null
                                && cita.getId().equals(id)
                )
                .findFirst();
    }

    @Override
    public List<Cita> findAll() {

        return new ArrayList<>(
                loadData().getCitas()
        );
    }

    @Override
    public void deleteById(Long id) {

        if (id == null) {
            return;
        }

        CitaData data = loadData();

        data.getCitas().removeIf(
                cita ->
                        cita.getId() != null
                                && cita.getId().equals(id)
        );

        saveData(data);
    }

    private CitaData loadData() {

        CitaData data = storage.load(
                filePath,
                CitaData.class,
                PASSWORD
        );

        if (data == null) {
            return new CitaData();
        }

        if (data.getCitas() == null) {
            data.setCitas(new ArrayList<>());
        }

        return data;
    }

    private void saveData(CitaData data) {

        storage.save(
                filePath,
                data,
                PASSWORD
        );
    }
}