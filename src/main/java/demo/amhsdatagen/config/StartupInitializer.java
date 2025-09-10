package demo.amhsdatagen.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public StartupInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // H2 데이터베이스에 즉시 연결 (초기화 트리거)
        jdbcTemplate.execute("SELECT 1");
    }
}


