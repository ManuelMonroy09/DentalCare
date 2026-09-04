package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.financiero.Cargo;
import java.util.ArrayList;
import java.util.List;

public class CargoData {
    private List<Cargo> cargos = new ArrayList<>();
    public List<Cargo> getCargos() { return cargos; }
    public void setCargos(List<Cargo> cargos) { this.cargos = cargos != null ? cargos : new ArrayList<>(); }
}
