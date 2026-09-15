package mx.dentalcare.security;

import java.util.EnumSet;
import java.util.Set;

public final class AuthenticatedUser {

    private final String username;
    private final String displayName;
    private final UserRole role;

    public AuthenticatedUser(String username, String displayName, UserRole role) {
        this.username = username;
        this.displayName = displayName;
        this.role = role;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public boolean isAdmin() {
        return role == UserRole.ADMINISTRADOR;
    }

    public boolean hasPermission(UserPermission permission) {
        return permissionsFor(role).contains(permission);
    }

    public static Set<UserPermission> permissionsFor(UserRole role) {
        if (role == UserRole.ADMINISTRADOR) {
            return EnumSet.allOf(UserPermission.class);
        }

        return EnumSet.of(
                UserPermission.VER_INICIO,
                UserPermission.VER_PACIENTES,
                UserPermission.GESTIONAR_CITAS
        );
    }
}
