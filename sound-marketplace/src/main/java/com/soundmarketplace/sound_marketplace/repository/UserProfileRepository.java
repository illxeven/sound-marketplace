package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UserProfileRepository extends JpaRepository<UserProfile, Integer> {

    @Query("SELECT up FROM UserProfile up WHERE up.userAccount.id = :userAccountId")
    Optional<UserProfile> findByUserAccountId(@Param("userAccountId") Integer userAccountId);
}
