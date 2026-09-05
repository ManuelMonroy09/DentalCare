package mx.dentalcare.infrastructure.persistence.encrypted;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.config.DataDirectoryService;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.infrastructure.persistence.file.PacienteData;
import mx.dentalcare.repository.PacienteRepository;
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
public class EncryptedFilePacienteRepository implements PacienteRepository {

    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;
    private final Path filePath = DataDirectoryService.resolve("pacientes.dat");

    public EncryptedFilePacienteRepository(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    @Override public Paciente save(Paciente paciente) {
        PacienteData data = loadData();
        if (paciente.getId() == null) {
            Long nuevoId = 1L;
            boolean idExiste;
            do {
                final Long idBuscado = nuevoId;
                idExiste = data.getPacientes().stream().anyMatch(p -> p.getId() != null && p.getId().equals(idBuscado));
                if (idExiste) nuevoId++;
            } while (idExiste);
            paciente.setId(nuevoId);
        }
        data.getPacientes().removeIf(p -> p.getId() != null && p.getId().equals(paciente.getId()));
        data.getPacientes().add(paciente);
        saveData(data);
        return paciente;
    }

    @Override public Optional<Paciente> findById(Long id) {
        if (id == null) return Optional.empty();
        return loadData().getPacientes().stream().filter(p -> p.getId() != null && p.getId().equals(id)).findFirst();
    }

    @Override public List<Paciente> findAll() { return new ArrayList<>(loadData().getPacientes()); }

    @Override public void deleteById(Long id) {
        if (id == null) return;
        PacienteData data = loadData();
        data.getPacientes().removeIf(p -> p.getId() != null && p.getId().equals(id));
        saveData(data);
    }

    private PacienteData loadData() {
        SecretKey masterKey = securitySession.requireMasterKey();
        PacienteData data = storage.load(filePath, PacienteData.class, masterKey);
        if (data == null) return new PacienteData();
        if (data.getPacientes() == null) data.setPacientes(new ArrayList<>());
        return data;
    }

    private void saveData(PacienteData data) { storage.save(filePath, data, securitySession.requireMasterKey()); }
}
