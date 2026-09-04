package mx.dentalcare.repository;

import mx.dentalcare.domain.financiero.Pago;
import java.util.List;
import java.util.Optional;

public interface PagoRepository {
    List<Pago> findAll();
    Optional<Pago> findById(Long id);
    Pago save(Pago pago);
}
