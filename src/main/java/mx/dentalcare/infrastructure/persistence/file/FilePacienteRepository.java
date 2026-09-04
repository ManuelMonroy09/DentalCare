package mx.dentalcare.infrastructure.persistence.file;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.repository.PacienteRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Repository;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

//@Repository
//@Primary
public class FilePacienteRepository implements PacienteRepository {
    private final JsonFileStorage storage;
    private final Path filePath;

    public FilePacienteRepository(ObjectMapper objectMapper, Path pacienteFilePath){
        this.storage = new JsonFileStorage(objectMapper);
        this.filePath = pacienteFilePath;
    }

    @Override
    public List<Paciente> findAll() {
        PacienteData data = storage.load(filePath, PacienteData.class);
        if(data == null){
            return new ArrayList<>();
        }
        return new ArrayList<>(data.getPacientes());
    }

    @Override
    public Optional<Paciente> findById(Long id) {
        return findAll().stream().filter(paciente -> paciente.getId().equals(id)).findFirst();
    }

    @Override
    public Paciente save(Paciente paciente) {
        List<Paciente> pacientes = findAll();
        pacientes.removeIf(existing -> existing.getId().equals(paciente.getId()));
        pacientes.add(paciente);
        PacienteData data = new PacienteData();
        data.setPacientes(pacientes);
        storage.save(filePath, data);
        return paciente;
    }

    @Override
    public void deleteById(Long id) {
        List<Paciente>pacientes = findAll();
        pacientes.removeIf(paciente -> paciente.getId().equals(id));
        PacienteData data = new PacienteData();
        data.setPacientes(pacientes);
        storage.save(filePath, data);
    }
}
