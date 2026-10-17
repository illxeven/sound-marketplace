package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Merchandise;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Integer> {

    @Query("SELECT m FROM Merchandise m WHERE m.seller.id = :sellerId")
    List<Merchandise> findBySellerId(@Param("sellerId") Integer sellerId);

    @Query("SELECT m FROM Merchandise m WHERE m.merchandiseType.id = :merchandiseTypeId")
    List<Merchandise> findByMerchandiseTypeId(@Param("merchandiseTypeId") Integer merchandiseTypeId);

    @Query("SELECT m FROM Merchandise m WHERE m.category.id = :categoryId")
    List<Merchandise> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT m FROM Merchandise m WHERE m.category.groupType.id = :groupTypeId")
    List<Merchandise> findByGroupTypeId(@Param("groupTypeId") Long groupTypeId);
}
