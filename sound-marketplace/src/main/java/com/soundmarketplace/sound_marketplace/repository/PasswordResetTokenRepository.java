package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.PasswordResetToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Integer> {
    Optional<PasswordResetToken> findByToken(String token);
    Optional<PasswordResetToken> findByTokenHash(String tokenHash);
    Optional<PasswordResetToken> findByTokenIgnoreCase(String token);
    java.util.List<PasswordResetToken> findByUserAccountId(Integer userAccountId);
}
