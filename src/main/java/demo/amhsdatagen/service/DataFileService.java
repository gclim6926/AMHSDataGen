package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class DataFileService {

    @Value("${app.data.dir:data}")
    private String dataDir;

    @Value("${app.files.input:input.json}")
    private String inputFileName;

    @Value("${app.files.output:output.json}")
    private String outputFileName;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Path getDataDir() {
        return Paths.get(dataDir).toAbsolutePath();
    }

    public Path getInputPath() {
        return getDataDir().resolve(inputFileName);
    }

    public Path getOutputPath() {
        return getDataDir().resolve(outputFileName);
    }

    public void ensureInputExistsFromSample() throws IOException {
        Path dir = getDataDir();
        if (!Files.exists(dir)) Files.createDirectories(dir);
        Path input = getInputPath();
        if (!Files.exists(input)) {
            ClassPathResource sample = new ClassPathResource("data/input.sample.json");
            try (InputStream in = sample.getInputStream()) {
                Files.copy(in, input);
            }
        }
    }

    public JsonNode readInput() throws IOException {
        ensureInputExistsFromSample();
        return objectMapper.readTree(getInputPath().toFile());
    }

    public void writeInput(JsonNode node) throws IOException {
        Path dir = getDataDir();
        if (!Files.exists(dir)) Files.createDirectories(dir);
        objectMapper.writerWithDefaultPrettyPrinter().writeValue(getInputPath().toFile(), node);
    }

    // H2 전용 모드: 샘플 JSON만 읽기 위한 헬퍼
    public JsonNode readSample() throws IOException {
        ClassPathResource sample = new ClassPathResource("data/input.sample.json");
        try (InputStream in = sample.getInputStream()) {
            return objectMapper.readTree(in);
        }
    }
}


