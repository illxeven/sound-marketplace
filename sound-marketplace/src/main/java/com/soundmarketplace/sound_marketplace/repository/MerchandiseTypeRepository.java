package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.MerchandiseType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface MerchandiseTypeRepository extends JpaRepository<MerchandiseType, Integer> {
    Optional<MerchandiseType> findByTypeNameIgnoreCase(String typeName);
}
