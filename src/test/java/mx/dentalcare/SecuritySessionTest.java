package mx.dentalcare;

import mx.dentalcare.security.AuthenticatedUser;
import mx.dentalcare.security.SecuritySession;
import mx.dentalcare.security.UserRole;
import org.junit.jupiter.api.Test;

import javax.crypto.spec.SecretKeySpec;

import static org.junit.jupiter.api.Assertions.*;

class SecuritySessionTest {

    @Test
    void debeRequerirAutenticacionAntesDeUsarLaSesion() {
        SecuritySession session = new SecuritySession();

        assertFalse(session.isAuthenticated());
        assertThrows(IllegalStateException.class, session::requireMasterKey);
        assertThrows(IllegalStateException.class, session::requireCurrentUser);
    }

    @Test
    void debeAutenticarYPermitirEstablecerUsuario() {
        SecuritySession session = new SecuritySession();
        SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");
        AuthenticatedUser user = new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR);

        session.authenticate(key);
        session.setCurrentUser(user);

        assertTrue(session.isAuthenticated());
        assertSame(key, session.requireMasterKey());
        assertSame(user, session.requireCurrentUser());
    }

    @Test
    void noDebeAceptarClaveMaestraNula() {
        SecuritySession session = new SecuritySession();
        assertThrows(IllegalArgumentException.class, () -> session.authenticate(null));
    }

    @Test
    void noDebePermitirUsuarioAntesDeAutenticar() {
        SecuritySession session = new SecuritySession();
        AuthenticatedUser user = new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR);
        SecretKeySpec key = new SecretKeySpec(new byte[32], "AES");

        assertThrows(IllegalStateException.class, () -> session.setCurrentUser(user));
        session.authenticate(key);
        session.setCurrentUser(user);
        assertSame(user, session.getCurrentUser());
    }

    @Test
    void clearDebeCerrarCompletamenteLaSesion() {
        SecuritySession session = new SecuritySession();
        session.authenticate(new SecretKeySpec(new byte[32], "AES"));
        session.setCurrentUser(new AuthenticatedUser("admin", "Administrador", UserRole.ADMINISTRADOR));

        session.clear();

        assertFalse(session.isAuthenticated());
        assertNull(session.getCurrentUser());
        assertThrows(IllegalStateException.class, session::requireMasterKey);
        assertThrows(IllegalStateException.class, session::requireCurrentUser);
    }
}
