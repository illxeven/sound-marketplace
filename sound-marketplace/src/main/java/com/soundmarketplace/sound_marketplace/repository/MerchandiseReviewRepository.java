package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.MerchandiseReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchandiseReviewRepository extends JpaRepository<MerchandiseReview, Integer> {

    List<MerchandiseReview> findByMerchandiseIdOrderByCreatedAtDesc(Integer merchandiseId);

    long countByMerchandiseId(Integer merchandiseId);

    @Query("SELECT AVG(r.rating) FROM MerchandiseReview r WHERE r.merchandise.id = :merchandiseId")
    Double getAverageRatingByMerchandiseId(@Param("merchandiseId") Integer merchandiseId);
}
