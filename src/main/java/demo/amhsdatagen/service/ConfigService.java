package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import demo.amhsdatagen.model.ConfigEntry;
import demo.amhsdatagen.repository.ConfigEntryRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

@Service
public class ConfigService {

    private static final String KEY_INPUT = "layout_seed.input";
    private static final String KEY_OUTPUT = "layout_seed.output";
    private static final String KEY_OHT_LOG = "oht_track.datalog";

    private final ConfigEntryRepository repository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigService(ConfigEntryRepository repository) {
        this.repository = repository;
    }

    public Optional<JsonNode> loadInputFromDb() {
        return repository.findByConfigKey(KEY_INPUT).map(entry -> {
            try {
                return objectMapper.readTree(entry.getConfigValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public void saveInputToDb(JsonNode node) {
        String json = node.toPrettyString();
        ConfigEntry entry = repository.findByConfigKey(KEY_INPUT)
                .orElseGet(() -> new ConfigEntry(KEY_INPUT, json));
        entry.setConfigValue(json);
        repository.save(entry);
    }

    public Optional<JsonNode> loadOutputFromDb() {
        return repository.findByConfigKey(KEY_OUTPUT).map(entry -> {
            try {
                return objectMapper.readTree(entry.getConfigValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public void saveOutputToDb(JsonNode node) {
        String json = node.toPrettyString();
        ConfigEntry entry = repository.findByConfigKey(KEY_OUTPUT)
                .orElseGet(() -> new ConfigEntry(KEY_OUTPUT, json));
        entry.setConfigValue(json);
        repository.save(entry);
    }

    public Optional<String> loadOhtLogFromDb() {
        return repository.findByConfigKey(KEY_OHT_LOG).map(ConfigEntry::getConfigValue);
    }

    @Transactional
    public void saveOhtLogToDb(String content) {
        String newChunk = content == null ? "" : content;
        ConfigEntry entry = repository.findByConfigKey(KEY_OHT_LOG)
                .orElseGet(() -> new ConfigEntry(KEY_OHT_LOG, ""));
        String prev = entry.getConfigValue();
        StringBuilder sb = new StringBuilder();
        if (prev != null && !prev.isEmpty()) {
            sb.append(prev);
            if (!prev.endsWith("\n")) sb.append('\n');
        }
        if (!newChunk.isEmpty()) sb.append(newChunk);
        entry.setConfigValue(sb.toString());
        repository.save(entry);
    }
}


