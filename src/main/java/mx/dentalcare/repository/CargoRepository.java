package mx.dentalcare.repository;

import mx.dentalcare.domain.financiero.Cargo;
import java.util.List;
import java.util.Optional;

public interface CargoRepository {
    List<Cargo> findAll();
    Optional<Cargo> findById(Long id);
    Optional<Cargo> findByCitaId(Long citaId);
    Cargo save(Cargo cargo);
}
