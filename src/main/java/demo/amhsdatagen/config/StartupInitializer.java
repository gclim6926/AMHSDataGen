package demo.amhsdatagen.config;

import com.fasterxml.jackson.databind.JsonNode;
import demo.amhsdatagen.service.ConfigService;
import demo.amhsdatagen.service.DataFileService;
import java.io.IOException;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final ConfigService configService;
    private final DataFileService dataFileService;

    public StartupInitializer(JdbcTemplate jdbcTemplate, ConfigService configService, DataFileService dataFileService) {
        this.jdbcTemplate = jdbcTemplate;
        this.configService = configService;
        this.dataFileService = dataFileService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // 1) H2 인메모리 DB에 즉시 연결 (초기화 트리거)
        jdbcTemplate.execute("SELECT 1");

        // 2) 필요 시 초기 데이터 주입
        initializeIfEmpty();
    }

    /**
     * DB에 기본 입력 데이터가 없으면 sample을 주입한다.
     */
    public void initializeIfEmpty() {
        try {
            if (configService.loadInputFromDb().isEmpty()) {
                JsonNode sample = dataFileService.readSample();
                configService.saveInputToDb(sample);
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        // layout_seed.output은 필요 시 각 기능에서 생성되므로 여기서는 강제 생성하지 않음
    }
}


