package demo.amhsdatagen.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class UserTableService {

    public final JdbcTemplate jdbcTemplate;
    private final DataFileService dataFileService;
    
    // 간단한 데이터 클래스
    public static class ConfigData {
        private Long id;
        private String configKey;
        private String configValue;
        
        public ConfigData() {}
        
        public ConfigData(Long id, String configKey, String configValue) {
            this.id = id;
            this.configKey = configKey;
            this.configValue = configValue;
        }
        
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public String getConfigKey() { return configKey; }
        public void setConfigKey(String configKey) { this.configKey = configKey; }
        public String getConfigValue() { return configValue; }
        public void setConfigValue(String configValue) { this.configValue = configValue; }
    }

    public UserTableService(JdbcTemplate jdbcTemplate, DataFileService dataFileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.dataFileService = dataFileService;
    }

    /**
     * UserID에 해당하는 테이블이 존재하는지 확인
     */
    public boolean isUserTableExists(String userId) {
        String tableName = userId + "_amhs_data";
        String sql = "SELECT COUNT(*) FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_NAME = ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, tableName.toUpperCase());
        return count != null && count > 0;
    }

    /**
     * 특정 config key의 값을 조회
     */
    public Optional<String> getConfigValue(String userId, String configKey) {
        if (!isUserTableExists(userId)) {
            return Optional.empty();
        }
        
        String tableName = userId + "_amhs_data";
        String sql = "SELECT CONFIG_VALUE FROM \"" + tableName + "\" WHERE CONFIG_KEY = ?";
        
        try {
            String value = jdbcTemplate.queryForObject(sql, String.class, configKey);
            return Optional.ofNullable(value);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * 사용자의 모든 config key 조회
     */
    public List<String> getAllConfigKeys(String userId) {
        if (!isUserTableExists(userId)) {
            return new ArrayList<>();
        }
        
        String tableName = userId + "_amhs_data";
        String sql = "SELECT CONFIG_KEY FROM \"" + tableName + "\"";
        
        try {
            return jdbcTemplate.queryForList(sql, String.class);
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    /**
     * 사용자 테이블의 데이터 개수 조회
     */
    public int getDataCount(String userId) {
        if (!isUserTableExists(userId)) {
            return 0;
        }
        
        String tableName = userId + "_amhs_data";
        String sql = "SELECT COUNT(*) FROM \"" + tableName + "\"";
        
        try {
            Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
            return count != null ? count : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * UserID에 해당하는 테이블 생성
     */
    @Transactional
    public void createUserTable(String userId) {
        String tableName = userId + "_amhs_data";
        String sql = String.format("""
            CREATE TABLE IF NOT EXISTS "%s" (
                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                config_key VARCHAR(128) NOT NULL UNIQUE,
                config_value CLOB NOT NULL
            )
            """, tableName);
        
        jdbcTemplate.execute(sql);
    }

    /**
     * UserID에 해당하는 테이블에 데이터가 있는지 확인
     */
    public boolean isUserTableEmpty(String userId) {
        String tableName = userId + "_amhs_data";
        String sql = String.format("SELECT COUNT(*) FROM \"%s\"", tableName);
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class);
        return count == null || count == 0;
    }

    /**
     * UserID에 해당하는 테이블에 샘플 데이터 주입
     */
    @Transactional
    public void initializeUserTableWithSample(String userId) {
        try {
            // 이미 데이터가 있는지 확인
            if (!isUserTableEmpty(userId)) {
                return; // 이미 데이터가 있으면 초기화하지 않음
            }
            
            JsonNode sample = dataFileService.readSample();
            String json = sample.toPrettyString();
            
            String tableName = userId + "_amhs_data";
            String sql = String.format("""
                INSERT INTO "%s" (config_key, config_value) 
                VALUES ('layout_seed.input', ?)
                """, tableName);
            
            jdbcTemplate.update(sql, json);
        } catch (Exception e) {
            throw new RuntimeException("샘플 데이터 초기화 실패: " + e.getMessage(), e);
        }
    }

    /**
     * UserID에 해당하는 테이블에서 데이터 조회
     */
    public Optional<ConfigData> findByUserIdAndKey(String userId, String key) {
        String tableName = userId + "_amhs_data";
        String sql = String.format("SELECT id, config_key, config_value FROM \"%s\" WHERE config_key = ?", tableName);
        
        try {
            ConfigData entry = jdbcTemplate.queryForObject(sql, (rs, rowNum) -> {
                ConfigData configData = new ConfigData();
                configData.setId(rs.getLong("id"));
                configData.setConfigKey(rs.getString("config_key"));
                configData.setConfigValue(rs.getString("config_value"));
                return configData;
            }, key);
            return Optional.ofNullable(entry);
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * UserID에 해당하는 테이블에 데이터 저장
     */
    @Transactional
    public void saveToUserTable(String userId, String key, String value) {
        String tableName = userId + "_amhs_data";
        // 기존 데이터 확인
        Optional<ConfigData> existing = findByUserIdAndKey(userId, key);
        
        if (existing.isPresent()) {
            // 업데이트
            String updateSql = String.format("UPDATE \"%s\" SET config_value = ? WHERE config_key = ?", tableName);
            jdbcTemplate.update(updateSql, value, key);
        } else {
            // 삽입
            String insertSql = String.format("INSERT INTO \"%s\" (config_key, config_value) VALUES (?, ?)", tableName);
            jdbcTemplate.update(insertSql, key, value);
        }
    }
}
