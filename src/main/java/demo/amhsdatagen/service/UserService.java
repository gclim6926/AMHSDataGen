package demo.amhsdatagen.service;

import demo.amhsdatagen.model.User;
import demo.amhsdatagen.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final UserTableService userTableService;

    public UserService(UserRepository userRepository, UserTableService userTableService) {
        this.userRepository = userRepository;
        this.userTableService = userTableService;
    }

    /**
     * 비밀번호 해시화
     */
    private String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(password.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("비밀번호 해시화 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * 사용자 등록
     */
    @Transactional
    public User registerUser(String userId, String password) {
        if (userRepository.existsByUserId(userId)) {
            throw new RuntimeException("이미 존재하는 UserID입니다: " + userId);
        }

        String hashedPassword = hashPassword(password);
        User user = new User(userId, hashedPassword);
        User savedUser = userRepository.save(user);

        // UserID에 해당하는 AMHS 데이터 테이블 생성 및 초기화
        userTableService.createUserTable(userId);
        userTableService.initializeUserTableWithSample(userId);

        return savedUser;
    }

    /**
     * 사용자 로그인
     */
    @Transactional
    public User loginUser(String userId, String password) {
        Optional<User> userOpt = userRepository.findByUserId(userId);
        if (userOpt.isEmpty()) {
            throw new RuntimeException("존재하지 않는 UserID입니다: " + userId);
        }

        User user = userOpt.get();
        if (!user.getIsActive()) {
            throw new RuntimeException("비활성화된 계정입니다: " + userId);
        }

        String hashedPassword = hashPassword(password);
        if (!hashedPassword.equals(user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }

        // 마지막 로그인 시간 업데이트
        user.setLastLoginAt(LocalDateTime.now());
        userRepository.save(user);

        return user;
    }

    /**
     * 사용자 존재 여부 확인
     */
    public boolean userExists(String userId) {
        return userRepository.existsByUserId(userId);
    }

    /**
     * 사용자 정보 조회
     */
    public Optional<User> findByUserId(String userId) {
        return userRepository.findByUserId(userId);
    }
}
