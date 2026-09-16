package mx.dentalcare.infrastructure.persistence.file;

import mx.dentalcare.domain.auditoria.AuditEntry;

import java.util.ArrayList;
import java.util.List;

public class AuditData {
    private List<AuditEntry> entries = new ArrayList<>();

    public List<AuditEntry> getEntries() { return entries; }
    public void setEntries(List<AuditEntry> entries) { this.entries = entries; }
}
