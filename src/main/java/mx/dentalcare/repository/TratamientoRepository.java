package mx.dentalcare.repository;

import mx.dentalcare.domain.tratamiento.Tratamiento;

import java.util.List;
import java.util.Optional;

public interface TratamientoRepository {

    Tratamiento save(Tratamiento tratamiento);

    Optional<Tratamiento> findById(Long id);

    List<Tratamiento> findAll();

    void deleteById(Long id);
}