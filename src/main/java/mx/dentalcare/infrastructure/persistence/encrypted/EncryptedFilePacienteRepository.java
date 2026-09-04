package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.infrastructure.persistence.file.PacienteData;
import mx.dentalcare.repository.PacienteRepository;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.EncryptedFileStorage;
import mx.dentalcare.security.KeyDerivationService;
import org.springframework.stereotype.Repository;
import org.springframework.context.annotation.Primary;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
@Primary
public class EncryptedFilePacienteRepository implements PacienteRepository {

    private static final String PASSWORD = "TemporalDentalCare2026";
    private final EncryptedFileStorage storage;
    private final Path filePath = Path.of("data", "pacientes.dat");
    public EncryptedFilePacienteRepository(ObjectMapper objectMapper) {
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override
    public Paciente save(Paciente paciente) {
        PacienteData data = loadData();
        if(paciente.getId()==null){
            Long nuevoId = 1L;
            boolean idExiste;
            do {
                idExiste = false;
                for(Paciente p : data.getPacientes()){
                    if(p.getId() != null && p.getId().equals(nuevoId)){
                        idExiste = true;
                        break;
                    }
                }
                if(idExiste){
                    nuevoId++;
                }
            } while(idExiste);
            paciente.setId(nuevoId);
        }
        data.getPacientes().removeIf(p -> p.getId() != null && p.getId().equals(paciente.getId()));
        data.getPacientes().add(paciente);
        saveData(data);
        return paciente;
    }

    @Override
    public Optional<Paciente> findById(Long id) {
        return loadData().getPacientes().stream().filter(p -> p.getId().equals(id)).findFirst();
    }

    @Override
    public List<Paciente> findAll() {
        return new ArrayList<>(loadData().getPacientes());
    }

    @Override
    public void deleteById(Long id) {
        PacienteData data = loadData();
        data.getPacientes().removeIf(p -> p.getId().equals(id));
        saveData(data);
    }

    private PacienteData loadData() {
        PacienteData data = storage.load(filePath, PacienteData.class, PASSWORD);
        if (data == null) {
            return new PacienteData();
        }
        return data;
    }

    private void saveData(PacienteData data) {
        storage.save(filePath, data, PASSWORD);
    }
}