package mx.dentalcare.security;

/**
 * Metadatos necesarios para desbloquear la clave maestra.
 * No contiene la contraseña del administrador.
 */
public class AuthenticationData {

    private int version;
    private int kdfIterations;
    private String salt;
    private String iv;
    private String wrappedMasterKey;

    public AuthenticationData() {
    }

    public AuthenticationData(int version, int kdfIterations, String salt, String iv, String wrappedMasterKey) {
        this.version = version;
        this.kdfIterations = kdfIterations;
        this.salt = salt;
        this.iv = iv;
        this.wrappedMasterKey = wrappedMasterKey;
    }

    public int getVersion() {
        return version;
    }

    public void setVersion(int version) {
        this.version = version;
    }

    public int getKdfIterations() {
        return kdfIterations;
    }

    public void setKdfIterations(int kdfIterations) {
        this.kdfIterations = kdfIterations;
    }

    public String getSalt() {
        return salt;
    }

    public void setSalt(String salt) {
        this.salt = salt;
    }

    public String getIv() {
        return iv;
    }

    public void setIv(String iv) {
        this.iv = iv;
    }

    public String getWrappedMasterKey() {
        return wrappedMasterKey;
    }

    public void setWrappedMasterKey(String wrappedMasterKey) {
        this.wrappedMasterKey = wrappedMasterKey;
    }
}
