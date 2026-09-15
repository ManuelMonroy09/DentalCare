package mx.dentalcare.security;

import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class SecuritySession {

    private SecretKey masterKey;
    private AuthenticatedUser currentUser;
    private boolean authenticated;

    public synchronized void authenticate(SecretKey masterKey) {
        if (masterKey == null) {
            throw new IllegalArgumentException("La clave maestra no puede ser nula.");
        }

        this.masterKey = masterKey;
        this.authenticated = true;
    }

    public synchronized void setCurrentUser(AuthenticatedUser user) {
        if (!authenticated || masterKey == null) {
            throw new IllegalStateException("DentalCare no está autenticado.");
        }
        this.currentUser = user;
    }

    public synchronized SecretKey requireMasterKey() {
        if (!authenticated || masterKey == null) {
            throw new IllegalStateException("DentalCare no está autenticado.");
        }

        return masterKey;
    }

    public synchronized AuthenticatedUser requireCurrentUser() {
        if (!authenticated || masterKey == null || currentUser == null) {
            throw new IllegalStateException("No hay un usuario autenticado.");
        }
        return currentUser;
    }

    public synchronized AuthenticatedUser getCurrentUser() {
        return currentUser;
    }

    public synchronized boolean isAuthenticated() {
        return authenticated && masterKey != null && currentUser != null;
    }

    public synchronized void clear() {
        masterKey = null;
        currentUser = null;
        authenticated = false;
    }
}
