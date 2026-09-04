package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.tratamiento.Tratamiento;

import java.util.ArrayList;
import java.util.List;

public class TratamientoData {

    private List<Tratamiento> tratamientos = new ArrayList<>();

    public List<Tratamiento> getTratamientos() {
        return tratamientos;
    }

    public void setTratamientos(List<Tratamiento> tratamientos) {
        this.tratamientos = tratamientos;
    }
}