package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.Optional;

@Service
public class ConfigService {

    private static final String KEY_INPUT = "layout_seed.input";
    private static final String KEY_OUTPUT = "layout_seed.output";
    private static final String KEY_OHT_LOG = "oht_track.datalog";

    private final UserTableService userTableService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ConfigService(UserTableService userTableService) {
        this.userTableService = userTableService;
    }

    // 사용자별 데이터 로드/저장 메서드들
    public Optional<JsonNode> loadInputFromDb(String userId) {
        return userTableService.findByUserIdAndKey(userId, KEY_INPUT).map(entry -> {
            try {
                return objectMapper.readTree(entry.getConfigValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public void saveInputToDb(String userId, JsonNode node) {
        String json = node.toPrettyString();
        userTableService.saveToUserTable(userId, KEY_INPUT, json);
    }

    public Optional<JsonNode> loadOutputFromDb(String userId) {
        return userTableService.findByUserIdAndKey(userId, KEY_OUTPUT).map(entry -> {
            try {
                return objectMapper.readTree(entry.getConfigValue());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        });
    }

    @Transactional
    public void saveOutputToDb(String userId, JsonNode node) {
        String json = node.toPrettyString();
        userTableService.saveToUserTable(userId, KEY_OUTPUT, json);
    }

    public Optional<String> loadOhtLogFromDb(String userId) {
        return userTableService.findByUserIdAndKey(userId, KEY_OHT_LOG).map(entry -> entry.getConfigValue());
    }

    @Transactional
    public void saveOhtLogToDb(String userId, String content) {
        String newChunk = content == null ? "" : content;
        Optional<UserTableService.ConfigData> existing = userTableService.findByUserIdAndKey(userId, KEY_OHT_LOG);
        
        String prev = existing.map(UserTableService.ConfigData::getConfigValue).orElse("");
        StringBuilder sb = new StringBuilder();
        if (prev != null && !prev.isEmpty()) {
            sb.append(prev);
            if (!prev.endsWith("\n")) sb.append('\n');
        }
        if (!newChunk.isEmpty()) sb.append(newChunk);
        
        userTableService.saveToUserTable(userId, KEY_OHT_LOG, sb.toString());
    }
    
    // 기존 메서드들 (사용자 ID 없이 호출되는 경우를 위한 기본값)
    public Optional<JsonNode> loadInputFromDb() {
        return Optional.empty();
    }

    @Transactional
    public void saveInputToDb(JsonNode node) {
        // 사용자 ID가 없는 경우 무시
    }

    public Optional<JsonNode> loadOutputFromDb() {
        return Optional.empty();
    }

    @Transactional
    public void saveOutputToDb(JsonNode node) {
        // 사용자 ID가 없는 경우 무시
    }

    public Optional<String> loadOhtLogFromDb() {
        return Optional.empty();
    }

    @Transactional
    public void saveOhtLogToDb(String content) {
        // 사용자 ID가 없는 경우 무시
    }
}


