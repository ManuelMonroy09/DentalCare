package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.cita.Cita;

import java.util.ArrayList;
import java.util.List;

public class CitaData {

    private List<Cita> citas;

    public CitaData() {
        this.citas = new ArrayList<>();
    }

    public List<Cita> getCitas() {
        return citas;
    }

    public void setCitas(List<Cita> citas) {
        this.citas = citas;
    }
}