package mx.dentalcare.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class SecuritySession {

    private SecretKey masterKey;
    private boolean authenticated;

    public synchronized void authenticate(SecretKey masterKey) {
        if (masterKey == null) {
            throw new IllegalArgumentException("La clave maestra no puede ser nula.");
        }

        this.masterKey = masterKey;
        this.authenticated = true;
    }

    public synchronized SecretKey requireMasterKey() {
        if (!authenticated || masterKey == null) {
            throw new IllegalStateException("DentalCare no está autenticado.");
        }

        return masterKey;
    }

    public synchronized boolean isAuthenticated() {
        return authenticated && masterKey != null;
    }

    public synchronized void clear() {
        masterKey = null;
        authenticated = false;
    }
}
