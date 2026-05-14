package com.actvn.enotary.repository;

import com.actvn.enotary.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {
    Optional<RefreshToken> findByJti(String jti);
    Optional<RefreshToken> findByToken(String token);
    void deleteByJti(String jti);
    List<RefreshToken> findByEmail(String email);
}
