package com.example.nl2sql.semantic;

import com.example.nl2sql.core.semantic.SemanticModels;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.LoaderOptions;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.io.OutputStream;

public class RegistryLoader {
  public SemanticModels.Registry loadFromClasspath(String resource) {
    InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
    if (is == null) throw new IllegalArgumentException("Resource not found: " + resource);
    return load(is);
  }

  public SemanticModels.Registry loadFromFile(Path file) {
    try (InputStream is = Files.newInputStream(file)) {
      return load(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private SemanticModels.Registry load(InputStream is) {
    Yaml yaml = new Yaml(new Constructor(SemanticModels.Registry.class, new LoaderOptions()));
    return yaml.load(is);
  }

   public void saveToFile(Path file, SemanticModels.Registry registry) {
     try (OutputStream os = Files.newOutputStream(file)) {
       Yaml yaml = new Yaml();
       String out = yaml.dump(registry);
       os.write(out.getBytes(java.nio.charset.StandardCharsets.UTF_8));
     } catch (Exception e) {
       throw new RuntimeException(e);
     }
   }
}
