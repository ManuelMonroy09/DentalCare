package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.paciente.Paciente;

import java.util.ArrayList;
import java.util.List;

public class PacienteData {
    private List<Paciente> pacientes = new ArrayList<>();

    public PacienteData(){
    }

    public List<Paciente> getPacientes(){
        return pacientes;
    }

    public void setPacientes(List<Paciente> pacientes){
        this.pacientes = pacientes;
    }
}
