package mx.dentalcare.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import mx.dentalcare.domain.configuracion.ConfiguracionConsultorio;
import mx.dentalcare.security.AesEncryptionService;
import mx.dentalcare.security.EncryptedFileStorage;
import mx.dentalcare.security.KeyDerivationService;
import mx.dentalcare.security.SecuritySession;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.file.Path;

@Service
public class ConfiguracionService {
    private static final Path FILE_PATH = Path.of("data", "configuracion.dat");

    private final EncryptedFileStorage storage;
    private final SecuritySession securitySession;

    public ConfiguracionService(ObjectMapper objectMapper, SecuritySession securitySession) {
        this.securitySession = securitySession;
        this.storage = new EncryptedFileStorage(objectMapper, new KeyDerivationService(), new AesEncryptionService());
    }

    public ConfiguracionConsultorio obtener() {
        SecretKey masterKey = securitySession.requireMasterKey();
        ConfiguracionConsultorio configuracion = storage.load(FILE_PATH, ConfiguracionConsultorio.class, masterKey);
        return configuracion == null ? new ConfiguracionConsultorio() : configuracion;
    }

    public void guardar(ConfiguracionConsultorio configuracion) {
        if (configuracion == null) throw new IllegalArgumentException("La configuración no puede ser nula.");
        if (configuracion.getNombreConsultorio() == null || configuracion.getNombreConsultorio().isBlank()) {
            throw new IllegalArgumentException("El nombre del consultorio es obligatorio.");
        }
        configuracion.setNombreConsultorio(limpiar(configuracion.getNombreConsultorio()));
        configuracion.setNombreOdontologo(limpiar(configuracion.getNombreOdontologo()));
        configuracion.setTelefono(limpiar(configuracion.getTelefono()));
        configuracion.setEmail(limpiar(configuracion.getEmail()));
        configuracion.setDireccion(limpiar(configuracion.getDireccion()));
        configuracion.setPieRecibo(limpiar(configuracion.getPieRecibo()));
        storage.save(FILE_PATH, configuracion, securitySession.requireMasterKey());
    }

    private String limpiar(String valor) {
        return valor == null ? "" : valor.trim();
    }
}
