package mx.dentalcare.service;

import mx.dentalcare.domain.tratamiento.Tratamiento;
import mx.dentalcare.repository.TratamientoRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TratamientoService {

    private final TratamientoRepository tratamientoRepository;

    public TratamientoService(TratamientoRepository tratamientoRepository) {
        this.tratamientoRepository = tratamientoRepository;
    }

    public List<Tratamiento> obtenerTodos() {
        return tratamientoRepository.findAll();
    }

    public List<Tratamiento> obtenerActivos() {
        return tratamientoRepository.findAll().stream().filter(Tratamiento::isActivo).toList();
    }

    public Tratamiento obtenerPorId(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("El ID del tratamiento es obligatorio.");
        }
        return tratamientoRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("No se encontró el tratamiento con ID: " + id));
    }

    public Tratamiento crear(Tratamiento tratamiento) {
        if (tratamiento == null) {
            throw new IllegalArgumentException("El tratamiento no puede ser nulo.");
        }
        tratamiento.setId(null);
        tratamiento.setActivo(true);
        tratamiento.validar();
        return tratamientoRepository.save(tratamiento);
    }

    public Tratamiento actualizar(Tratamiento tratamiento) {
        if (tratamiento == null) {
            throw new IllegalArgumentException("El tratamiento no puede ser nulo.");
        }
        if (tratamiento.getId() == null) {
            throw new IllegalArgumentException("El tratamiento debe tener un ID para actualizarse.");
        }
        obtenerPorId(tratamiento.getId());
        tratamiento.validar();
        return tratamientoRepository.save(tratamiento);
    }

    public void activar(Long id) {
        Tratamiento tratamiento = obtenerPorId(id);
        tratamiento.activar();
        tratamientoRepository.save(tratamiento);
    }

    public void desactivar(Long id) {
        Tratamiento tratamiento = obtenerPorId(id);
        tratamiento.desactivar();
        tratamientoRepository.save(tratamiento);
    }
}