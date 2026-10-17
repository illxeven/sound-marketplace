package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Entitlement;
import com.soundmarketplace.sound_marketplace.domain.EntitlementId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EntitlementRepository extends JpaRepository<Entitlement, EntitlementId> {
    boolean existsByUserIdAndTrackId(Integer userId, Integer trackId);
    Page<Entitlement> findByUserIdOrderByGrantedAtDesc(Integer userId, Pageable pageable);
    void deleteByOrderItemId(Long orderItemId);
}
