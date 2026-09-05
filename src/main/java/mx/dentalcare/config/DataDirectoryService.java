package mx.dentalcare.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.stream.Stream;

/**
 * Centraliza la ubicación de los datos persistentes de DentalCare.
 *
 * En Windows los datos viven fuera de la carpeta de instalación para que
 * actualizar o desinstalar la aplicación no dependa de permisos de escritura
 * sobre Program Files y no elimine los datos del consultorio.
 */
public final class DataDirectoryService {

    private static final String APP_NAME = "DentalCare";
    private static final String DATA_DIR_PROPERTY = "dentalcare.data.dir";
    private static final Path DATA_DIRECTORY = initialize();

    private DataDirectoryService() {
    }

    public static Path directory() {
        return DATA_DIRECTORY;
    }

    public static Path resolve(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("El nombre del archivo de datos es obligatorio.");
        }
        return DATA_DIRECTORY.resolve(fileName);
    }

    private static Path initialize() {
        String configuredDirectory = System.getProperty(DATA_DIR_PROPERTY);
        Path directory;

        if (configuredDirectory != null && !configuredDirectory.isBlank()) {
            directory = Path.of(configuredDirectory).toAbsolutePath().normalize();
        } else if (isWindows()) {
            String localAppData = System.getenv("LOCALAPPDATA");
            if (localAppData != null && !localAppData.isBlank()) {
                directory = Path.of(localAppData, APP_NAME, "data")
                        .toAbsolutePath()
                        .normalize();
            } else {
                directory = Path.of("data").toAbsolutePath().normalize();
            }
        } else {
            directory = Path.of("data").toAbsolutePath().normalize();
        }

        try {
            migrateLegacyDirectoryIfNecessary(directory);
            Files.createDirectories(directory);
            return directory;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "No fue posible preparar la carpeta de datos de DentalCare.",
                    e
            );
        }
    }

    private static void migrateLegacyDirectoryIfNecessary(Path target) throws IOException {
        if (!isWindows() || Files.exists(target)) {
            return;
        }

        Path legacy = Path.of("data").toAbsolutePath().normalize();
        if (!Files.isDirectory(legacy) || legacy.equals(target)) {
            return;
        }

        Files.createDirectories(target);

        try (Stream<Path> files = Files.walk(legacy)) {
            files.filter(Files::isRegularFile).forEach(source -> {
                Path relative = legacy.relativize(source);
                Path destination = target.resolve(relative).normalize();
                try {
                    Files.createDirectories(destination.getParent());
                    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
                } catch (IOException e) {
                    throw new DataMigrationException(e);
                }
            });
        } catch (DataMigrationException e) {
            throw e.getCause();
        }
    }

    private static boolean isWindows() {
        return System.getProperty("os.name", "")
                .toLowerCase()
                .contains("win");
    }

    private static final class DataMigrationException extends RuntimeException {
        private DataMigrationException(IOException cause) {
            super(cause);
        }

        @Override
        public synchronized IOException getCause() {
            return (IOException) super.getCause();
        }
    }
}
