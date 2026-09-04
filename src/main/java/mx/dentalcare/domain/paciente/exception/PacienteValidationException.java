package mx.dentalcare.domain.paciente.exception;

public class PacienteValidationException extends RuntimeException {
    public PacienteValidationException(String message){
        super(message);
    }
}
