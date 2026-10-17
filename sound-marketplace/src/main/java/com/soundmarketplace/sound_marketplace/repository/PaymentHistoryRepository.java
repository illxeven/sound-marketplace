package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.PaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PaymentHistoryRepository extends JpaRepository<PaymentHistory, Integer> {

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.buyer.id = :buyerId")
    List<PaymentHistory> findByBuyerId(@Param("buyerId") Integer buyerId);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.merchandise.id = :merchandiseId")
    List<PaymentHistory> findByMerchandiseId(@Param("merchandiseId") Integer merchandiseId);

    @Query("SELECT ph FROM PaymentHistory ph WHERE ph.track.id = :trackId")
    List<PaymentHistory> findByTrackId(@Param("trackId") Integer trackId);
}
