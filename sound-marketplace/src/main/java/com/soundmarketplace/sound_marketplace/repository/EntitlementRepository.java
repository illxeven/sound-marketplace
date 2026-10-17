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

    @org.springframework.data.jpa.repository.Query("SELECT e.track FROM Entitlement e WHERE e.user.id = :userId ORDER BY e.grantedAt DESC")
    Page<com.soundmarketplace.sound_marketplace.domain.Track> findTracksByUserId(@org.springframework.data.repository.query.Param("userId") Integer userId, Pageable pageable);

    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.data.jpa.repository.Query("DELETE FROM Entitlement e WHERE e.orderItem.id IN :orderItemIds")
    void deleteAllByOrderItemIdIn(@org.springframework.data.repository.query.Param("orderItemIds") java.util.Collection<Long> orderItemIds);
}
