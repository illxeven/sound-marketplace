package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByTokenHash(String tokenHash);
    void deleteByUserId(Integer userId);
    java.util.List<RefreshToken> findByUser_Id(Integer userId);

    @org.springframework.data.jpa.repository.Modifying(clearAutomatically = true)
    @org.springframework.data.jpa.repository.Query("UPDATE RefreshToken r SET r.revokedAt = CURRENT_TIMESTAMP WHERE r.user.id = :userId AND r.revokedAt IS NULL")
    void revokeAllByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId);
}
