package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAccountRepository extends JpaRepository<UserAccount, Integer> {
    Optional<UserAccount> findByEmail(String email);
    Optional<UserAccount> findByVerificationToken(String token);
}
