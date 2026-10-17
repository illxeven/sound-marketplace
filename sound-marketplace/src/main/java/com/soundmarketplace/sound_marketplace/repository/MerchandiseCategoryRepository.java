package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.MerchandiseCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MerchandiseCategoryRepository extends JpaRepository<MerchandiseCategory, Long> {
    List<MerchandiseCategory> findByGroupTypeId(Long groupTypeId);
    Optional<MerchandiseCategory> findByNameIgnoreCase(String name);
}
