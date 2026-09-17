package mx.dentalcare;

import mx.dentalcare.domain.configuracion.ConfiguracionConsultorio;
import mx.dentalcare.security.SecuritySession;
import mx.dentalcare.service.AuditService;
import mx.dentalcare.service.ConfiguracionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ConfiguracionServiceTest {

    private ConfiguracionService service;
    private SecuritySession securitySession;
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        securitySession = mock(SecuritySession.class);
        auditService = mock(AuditService.class);
        service = new ConfiguracionService(new ObjectMapper(), securitySession, auditService);
    }

    @Test
    void debeRechazarConfiguracionNula() {
        assertThrows(IllegalArgumentException.class, () -> service.guardar(null));
        verifyNoInteractions(securitySession, auditService);
    }

    @Test
    void debeRechazarNombreDeConsultorioVacio() {
        ConfiguracionConsultorio configuracion = new ConfiguracionConsultorio();
        configuracion.setNombreConsultorio("   ");

        assertThrows(IllegalArgumentException.class, () -> service.guardar(configuracion));
        verifyNoInteractions(securitySession, auditService);
    }
}
