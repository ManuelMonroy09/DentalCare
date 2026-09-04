package mx.dentalcare.service;

import mx.dentalcare.domain.paciente.Paciente;
import mx.dentalcare.domain.paciente.exception.PacienteValidationException;
import mx.dentalcare.repository.PacienteRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PacientesService {

    private static final int MAX_NOMBRE = 50;
    private static final int MAX_APELLIDO = 50;
    private static final int MAX_TELEFONO = 10;
    private static final int MAX_EMAIL = 100;
    private static final String PATRON_NOMBRE = "^[\\p{L}]+(?:[ '\\-][\\p{L}]+)*$";
    private static final String PATRON_TELEFONO = "^\\d{10}$";
    private static final String PATRON_EMAIL = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";

    private final PacienteRepository pacienteRepository;

    public PacientesService(PacienteRepository pacienteRepository){
        this.pacienteRepository = pacienteRepository;
    }

    public List<Paciente> obtenerTodos(){
        return pacienteRepository.findAll();
    }

    public Optional<Paciente> obtenerPorId(Long id){
        return pacienteRepository.findById(id);
    }

    public Paciente guardar(Paciente paciente){
        validar(paciente);
        validarDuplicado(paciente);
        return pacienteRepository.save(paciente);
    }

    public void eliminar(Long id){
        pacienteRepository.deleteById(id);
    }

    private void validar(Paciente paciente){
        if(paciente == null){
            throw new PacienteValidationException("El paciente no puede ser nulo.");
        }

        validarNombre(paciente.getNombre());
        validarApellidoPaterno(paciente.getApellidoPaterno());
        validarApellidoMaterno(paciente.getApellidoMaterno());
        validarTelefono(paciente.getTelefono());
        validarEmail(paciente.getEmail());
    }

    private void validarNombre(String nombre){

        if(nombre == null || nombre.isBlank()){
            throw new PacienteValidationException("El nombre del paciente es obligatorio.");
        }
        String valor = nombre.trim();

        if(valor.length() < 2){
            throw new PacienteValidationException("El nombre debe contener al menos 2 caracteres.");
        }

        if(valor.length() > MAX_NOMBRE){
            throw new PacienteValidationException("El nombre no puede superar los 50 caracteres.");
        }

        if(!valor.matches(PATRON_NOMBRE)){
            throw new PacienteValidationException("El nombre solo puede contener letras, espacios, apóstrofes y guiones.");
        }
    }

    private void validarApellidoPaterno(String apellidoPaterno){
        if(apellidoPaterno == null || apellidoPaterno.isBlank()){
            throw new PacienteValidationException("El apellido paterno es obligatorio.");
        }
        String valor = apellidoPaterno.trim();

        if(valor.length() < 2){
            throw new PacienteValidationException("El apellido paterno debe contener al menos 2 caracteres.");
        }

        if(valor.length() > MAX_APELLIDO){
            throw new PacienteValidationException("El apellido paterno no puede superar los 50 caracteres.");
        }

        if(!valor.matches(PATRON_NOMBRE)){
            throw new PacienteValidationException("El apellido paterno solo puede contener letras, espacios, apóstrofes y guiones.");
        }
    }

    private void validarApellidoMaterno(String apellidoMaterno){

        if(apellidoMaterno == null || apellidoMaterno.isBlank()){
            throw new PacienteValidationException("El apellido materno es obligatorio.");
        }
        String valor = apellidoMaterno.trim();

        if(valor.length() < 2){
            throw new PacienteValidationException("El apellido materno debe contener al menos 2 caracteres.");
        }

        if(valor.length() > MAX_APELLIDO){
            throw new PacienteValidationException("El apellido materno no puede superar los 50 caracteres.");
        }

        if(!valor.matches(PATRON_NOMBRE)){
            throw new PacienteValidationException("El apellido materno solo puede contener letras, espacios, apóstrofes y guiones.");
        }
    }

    private void validarTelefono(String telefono){

        if(telefono == null || telefono.isBlank()){
            throw new PacienteValidationException("El teléfono es obligatorio.");
        }
        String valor = telefono.trim();

        if(valor.length() > MAX_TELEFONO){
            throw new PacienteValidationException("El teléfono debe contener exactamente 10 dígitos.");
        }

        if(!valor.matches(PATRON_TELEFONO)){
            throw new PacienteValidationException("El teléfono debe contener exactamente 10 dígitos.");
        }
    }

    private void validarEmail(String email){
        if(email == null || email.isBlank()){
            return;
        }
        String valor = email.trim();

        if(valor.length() > MAX_EMAIL){
            throw new PacienteValidationException("El correo electrónico no puede superar los 100 caracteres.");
        }

        if(!valor.matches(PATRON_EMAIL)){
            throw new PacienteValidationException("El correo electrónico no tiene un formato válido.");
        }
    }

    private void validarDuplicado(Paciente paciente){
        List<Paciente> pacientes = pacienteRepository.findAll();
        boolean duplicado = pacientes.stream().anyMatch(existente -> !esMismoPaciente(existente, paciente)
                                && mismoTexto(existente.getNombre(), paciente.getNombre())
                                && mismoTexto(existente.getApellidoPaterno(), paciente.getApellidoPaterno())
                                && mismoTexto(existente.getApellidoMaterno(), paciente.getApellidoMaterno())
                                && mismoTexto(existente.getTelefono(), paciente.getTelefono())
                );

        if(duplicado){
            throw new PacienteValidationException("Ya existe un paciente registrado con esos datos.");
        }
    }

    private boolean esMismoPaciente(Paciente existente, Paciente paciente){
        if(existente.getId() == null || paciente.getId() == null){
            return false;
        }

        return existente.getId().equals(paciente.getId());
    }

    private boolean mismoTexto(String valor1, String valor2){
        if(valor1 == null || valor2 == null){
            return valor1 == null && valor2 == null;
        }
        return valor1.trim().equalsIgnoreCase(valor2.trim());
    }
}