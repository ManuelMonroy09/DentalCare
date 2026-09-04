package mx.dentalcare.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.nio.file.Path;

@Configuration
public class PersistenceConfig {

    @Bean
    public Path pacienteFilePath() {
        return Path.of(
                "data",
                "pacientes.json"
        );
    }
}