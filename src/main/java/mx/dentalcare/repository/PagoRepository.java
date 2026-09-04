package mx.dentalcare.repository;

import mx.dentalcare.domain.financiero.Pago;
import java.util.List;

public interface PagoRepository {
    List<Pago> findAll();
    Pago save(Pago pago);
}
