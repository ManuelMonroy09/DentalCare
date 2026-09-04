package mx.dentalcare.domain.configuracion;

import java.io.Serializable;

public class ConfiguracionConsultorio implements Serializable {
    private static final long serialVersionUID = 1L;

    private String nombreConsultorio;
    private String nombreOdontologo;
    private String telefono;
    private String email;
    private String direccion;
    private String pieRecibo;

    public ConfiguracionConsultorio() {
        nombreConsultorio = "DentalCare";
        nombreOdontologo = "";
        telefono = "";
        email = "";
        direccion = "";
        pieRecibo = "Gracias por su visita.";
    }

    public String getNombreConsultorio() { return nombreConsultorio; }
    public void setNombreConsultorio(String nombreConsultorio) { this.nombreConsultorio = nombreConsultorio; }
    public String getNombreOdontologo() { return nombreOdontologo; }
    public void setNombreOdontologo(String nombreOdontologo) { this.nombreOdontologo = nombreOdontologo; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getPieRecibo() { return pieRecibo; }
    public void setPieRecibo(String pieRecibo) { this.pieRecibo = pieRecibo; }
}
