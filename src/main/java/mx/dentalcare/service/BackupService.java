package mx.dentalcare.service;

import mx.dentalcare.config.DataDirectoryService;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class BackupService {
    private static final Path DATA_DIRECTORY = DataDirectoryService.directory();
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

    public Path crearRespaldo(Path destino) {
        if (destino == null) throw new IllegalArgumentException("Debes seleccionar un archivo de respaldo.");
        try {
            Files.createDirectories(DATA_DIRECTORY);
            Path archivo = destino.toAbsolutePath().normalize();
            Path padre = archivo.getParent();
            if (padre != null) Files.createDirectories(padre);
            if (archivo.startsWith(DATA_DIRECTORY)) throw new IllegalArgumentException("El respaldo debe guardarse fuera de la carpeta de datos.");
            try (OutputStream output = Files.newOutputStream(archivo); ZipOutputStream zip = new ZipOutputStream(output);
                 var stream = Files.walk(DATA_DIRECTORY)) {
                stream.filter(Files::isRegularFile).forEach(path -> agregarArchivo(zip, path));
            }
            return archivo;
        } catch (IOException ex) { throw new IllegalStateException("No fue posible crear el respaldo: " + ex.getMessage(), ex); }
    }

    public Path crearRespaldoAutomatico(Path directorio) {
        if (directorio == null) throw new IllegalArgumentException("El directorio de respaldo es obligatorio.");
        return crearRespaldo(directorio.resolve("DentalCare_backup_" + LocalDateTime.now().format(TIMESTAMP) + ".zip"));
    }

    public void restaurarRespaldo(Path respaldo) {
        if (respaldo == null || !Files.isRegularFile(respaldo)) throw new IllegalArgumentException("El archivo de respaldo no existe.");
        Path temporal = null;
        try {
            temporal = Files.createTempDirectory("dentalcare-restore-");
            try (InputStream input = Files.newInputStream(respaldo); ZipInputStream zip = new ZipInputStream(input)) {
                ZipEntry entry;
                while ((entry = zip.getNextEntry()) != null) {
                    if (entry.isDirectory()) continue;
                    Path destino = temporal.resolve(entry.getName()).normalize();
                    if (!destino.startsWith(temporal)) throw new IllegalStateException("El respaldo contiene una ruta no válida.");
                    Path padre = destino.getParent();
                    if (padre != null) Files.createDirectories(padre);
                    Files.copy(zip, destino, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            Path archivoSeguridad = temporal.resolve("security.dat").normalize();
            if (!Files.isRegularFile(archivoSeguridad)) throw new IllegalArgumentException("El archivo seleccionado no es un respaldo válido de DentalCare.");
            limpiarDatosActuales();
            Path origenTemporal = temporal;
            try (var stream = Files.walk(origenTemporal)) {
                stream.filter(Files::isRegularFile).forEach(origen -> {
                    Path relativa = origenTemporal.relativize(origen);
                    Path destino = DATA_DIRECTORY.resolve(relativa).normalize();
                    try {
                        Files.createDirectories(destino.getParent());
                        Files.copy(origen, destino, StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException ex) { throw new IllegalStateException("No fue posible restaurar " + relativa + ".", ex); }
                });
            }
        } catch (IOException ex) { throw new IllegalStateException("No fue posible restaurar el respaldo: " + ex.getMessage(), ex); }
        finally { if (temporal != null) eliminarDirectorioTemporal(temporal); }
    }

    private void agregarArchivo(ZipOutputStream zip, Path archivo) {
        try {
            ZipEntry entry = new ZipEntry(DATA_DIRECTORY.relativize(archivo).toString().replace('\\', '/'));
            zip.putNextEntry(entry);
            Files.copy(archivo, zip);
            zip.closeEntry();
        } catch (IOException ex) { throw new IllegalStateException("No fue posible incluir " + archivo + " en el respaldo.", ex); }
    }

    private void limpiarDatosActuales() throws IOException {
        if (!Files.exists(DATA_DIRECTORY)) { Files.createDirectories(DATA_DIRECTORY); return; }
        try (var stream = Files.walk(DATA_DIRECTORY)) {
            stream.filter(Files::isRegularFile).forEach(path -> {
                try { Files.deleteIfExists(path); } catch (IOException ex) { throw new IllegalStateException("No fue posible limpiar " + path + ".", ex); }
            });
        }
    }

    private void eliminarDirectorioTemporal(Path directorio) {
        try (var stream = Files.walk(directorio)) {
            stream.sorted(Comparator.reverseOrder()).forEach(path -> { try { Files.deleteIfExists(path); } catch (IOException ignored) { } });
        } catch (IOException ignored) { }
    }
}
