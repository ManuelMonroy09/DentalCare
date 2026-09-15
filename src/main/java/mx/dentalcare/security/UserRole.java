package mx.dentalcare.security;

public enum UserRole {
    ADMINISTRADOR("Administrador"),
    USUARIO("Usuario");

    private final String displayName;

    UserRole(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
