package demo.amhsdatagen.repository;

import demo.amhsdatagen.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findByUserId(String userId);
    
    boolean existsByUserId(String userId);
    
    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.userId = :userId")
    void updateLastLoginAt(@Param("userId") String userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);
}
