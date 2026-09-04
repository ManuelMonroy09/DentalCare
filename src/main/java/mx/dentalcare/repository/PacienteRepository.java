package mx.dentalcare.repository;

 import mx.dentalcare.domain.paciente.Paciente;

 import java.util.List;
 import java.util.Optional;

public interface PacienteRepository {

    List<Paciente> findAll();

    Optional<Paciente> findById(Long id);

    Paciente save(Paciente paciente);

    void deleteById(Long id);
}
