package mx.dentalcare.infrastructure.persistence.file;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class JsonFileStorage {
    private final ObjectMapper objectMapper;

    public JsonFileStorage(ObjectMapper objectMapper){
        this.objectMapper = objectMapper;
    }

    public <T> void save (Path path, T data){
        try{
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), data);
        }catch(IOException e){
            throw new RuntimeException("No fue posible guardar el archivo.", e);
        }
    }

    public <T> T load(
            Path path,
            Class<T> type){
         try{
             if(!Files.exists(path)){
                 return null;
             }
             return objectMapper.readValue(path.toFile(), type);
         }catch (IOException e){
             throw new RuntimeException("No fue posible leer el archivo.", e);
         }
    }
}
