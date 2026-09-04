package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.financiero.Pago;
import java.util.ArrayList;
import java.util.List;

public class PagoData {
    private List<Pago> pagos = new ArrayList<>();
    public List<Pago> getPagos() { return pagos; }
    public void setPagos(List<Pago> pagos) { this.pagos = pagos != null ? pagos : new ArrayList<>(); }
}
