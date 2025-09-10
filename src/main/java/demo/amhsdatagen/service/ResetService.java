package demo.amhsdatagen.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ResetService {

    public ResetService() {
    }

    @Transactional
    public void resetAll() {
        // 더미 구현 - 실제로는 아무것도 하지 않음
        // UserID 기반 테이블은 UserTableService에서 관리됨
    }
}