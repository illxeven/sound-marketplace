package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.StripeEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface StripeEventRepository extends JpaRepository<StripeEvent, String> {

    @Modifying
    @Query(value = "INSERT IGNORE INTO stripe_events (id, type, received_at) VALUES (:id, :type, CURRENT_TIMESTAMP)", nativeQuery = true)
    int insertIgnore(@Param("id") String id, @Param("type") String type);
}
