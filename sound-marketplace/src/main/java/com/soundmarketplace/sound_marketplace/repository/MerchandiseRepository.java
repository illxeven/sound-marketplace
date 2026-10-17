package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.Merchandise;
import com.soundmarketplace.sound_marketplace.dto.MerchandiseSummaryDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MerchandiseRepository extends JpaRepository<Merchandise, Integer> {

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.MerchandiseSummaryDTO(" +
            "m.id, m.title, s.id, s.email, m.price, m.stockQuantity, " +
            "c.name, mt.typeName, CONCAT('/api/merchandise/', m.id, '/images/1'), " +
            "CAST(CASE WHEN m.imageBlob5 IS NOT NULL THEN 5 " +
            "     WHEN m.imageBlob4 IS NOT NULL THEN 4 " +
            "     WHEN m.imageBlob3 IS NOT NULL THEN 3 " +
            "     WHEN m.imageBlob2 IS NOT NULL THEN 2 " +
            "     WHEN m.imageBlob IS NOT NULL THEN 1 ELSE 0 END AS integer), " +
            "m.createdAt) " +
            "FROM Merchandise m " +
            "LEFT JOIN m.seller s " +
            "LEFT JOIN m.category c " +
            "LEFT JOIN m.merchandiseType mt",
            countQuery = "SELECT count(m) FROM Merchandise m")
    Page<MerchandiseSummaryDTO> findMerchandiseSummaries(Pageable pageable);

    @Query(value = "SELECT new com.soundmarketplace.sound_marketplace.dto.MerchandiseSummaryDTO(" +
            "m.id, m.title, s.id, s.email, m.price, m.stockQuantity, " +
            "c.name, mt.typeName, CONCAT('/api/merchandise/', m.id, '/images/1'), " +
            "CAST(CASE WHEN m.imageBlob5 IS NOT NULL THEN 5 " +
            "     WHEN m.imageBlob4 IS NOT NULL THEN 4 " +
            "     WHEN m.imageBlob3 IS NOT NULL THEN 3 " +
            "     WHEN m.imageBlob2 IS NOT NULL THEN 2 " +
            "     WHEN m.imageBlob IS NOT NULL THEN 1 ELSE 0 END AS integer), " +
            "m.createdAt) " +
            "FROM Merchandise m " +
            "LEFT JOIN m.seller s " +
            "LEFT JOIN m.category c " +
            "LEFT JOIN m.merchandiseType mt " +
            "WHERE s.id = :sellerId",
            countQuery = "SELECT count(m) FROM Merchandise m WHERE m.seller.id = :sellerId")
    Page<MerchandiseSummaryDTO> findMerchandiseSummariesBySellerId(@Param("sellerId") Integer sellerId, Pageable pageable);

    @Query("SELECT m FROM Merchandise m WHERE m.seller.id = :sellerId")
    List<Merchandise> findBySellerId(@Param("sellerId") Integer sellerId);

    @Query("SELECT m FROM Merchandise m WHERE m.merchandiseType.id = :merchandiseTypeId")
    List<Merchandise> findByMerchandiseTypeId(@Param("merchandiseTypeId") Integer merchandiseTypeId);

    @Query("SELECT m FROM Merchandise m WHERE m.category.id = :categoryId")
    List<Merchandise> findByCategoryId(@Param("categoryId") Long categoryId);

    @Query("SELECT m FROM Merchandise m WHERE m.category.groupType.id = :groupTypeId")
    List<Merchandise> findByGroupTypeId(@Param("groupTypeId") Long groupTypeId);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Merchandise m SET m.stockQuantity = m.stockQuantity - :quantity WHERE m.id = :id AND m.stockQuantity >= :quantity")
    int decrementStockIfAvailable(@Param("id") Integer id, @Param("quantity") Integer quantity);

    @Query("SELECT m.imageBlob FROM Merchandise m WHERE m.id = :id")
    byte[] findImageBlob1ById(@Param("id") Integer id);

    @Query("SELECT m.imageBlob2 FROM Merchandise m WHERE m.id = :id")
    byte[] findImageBlob2ById(@Param("id") Integer id);

    @Query("SELECT m.imageBlob3 FROM Merchandise m WHERE m.id = :id")
    byte[] findImageBlob3ById(@Param("id") Integer id);

    @Query("SELECT m.imageBlob4 FROM Merchandise m WHERE m.id = :id")
    byte[] findImageBlob4ById(@Param("id") Integer id);

    @Query("SELECT m.imageBlob5 FROM Merchandise m WHERE m.id = :id")
    byte[] findImageBlob5ById(@Param("id") Integer id);
}
