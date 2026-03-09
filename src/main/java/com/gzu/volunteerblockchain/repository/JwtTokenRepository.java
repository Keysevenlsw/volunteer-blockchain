package com.gzu.volunteerblockchain.repository;

import com.gzu.volunteerblockchain.entity.JwtToken;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JwtTokenRepository extends JpaRepository<JwtToken, Integer> {

    Optional<JwtToken> findTopByTokenOrderByTokenIdDesc(String token);

    void deleteByUserIdAndExpiresAtBefore(Integer userId, LocalDateTime time);
}
