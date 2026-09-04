package mx.dentalcare.infrastructure.persistence.memory;

import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.repository.PacienteRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class InMemoryPacienteRepository implements PacienteRepository {
    private final List<Paciente> pacientes = new ArrayList<>();

    @Override
    public List<Paciente> findAll(){
        return new ArrayList<>(pacientes);
    }

    @Override
    public Optional<Paciente> findById(Long id){
        return pacientes.stream()
                .filter(paciente -> paciente.getId().equals(id))
                .findFirst();
    }

    @Override
    public Paciente save(Paciente paciente){
        Optional<Paciente> existente = findById(paciente.getId());
        if(existente.isPresent()){
            pacientes.remove(existente.get());
        }
        pacientes.add(paciente);
        return paciente;
    }

    @Override
    public void deleteById(Long id) {
        pacientes.removeIf(paciente -> paciente.getId().equals(id));
    }
}
