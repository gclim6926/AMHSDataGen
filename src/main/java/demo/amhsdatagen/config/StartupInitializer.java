package demo.amhsdatagen.config;

import demo.amhsdatagen.service.UserService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupInitializer implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final UserService userService;

    public StartupInitializer(JdbcTemplate jdbcTemplate, UserService userService) {
        this.jdbcTemplate = jdbcTemplate;
        this.userService = userService;
    }

    @Override
    public void run(ApplicationArguments args) throws Exception {
        // H2 데이터베이스에 즉시 연결 (초기화 트리거)
        jdbcTemplate.execute("SELECT 1");
        
        // 수퍼유저 생성 (admin/admin)
        try {
            if (!userService.userExists("admin")) {
                userService.createSuperuser("admin", "admin");
                System.out.println("수퍼유저가 생성되었습니다: admin");
            } else {
                System.out.println("수퍼유저가 이미 존재합니다: admin");
            }
        } catch (Exception e) {
            System.err.println("수퍼유저 생성 중 오류가 발생했습니다: " + e.getMessage());
        }
    }
}


