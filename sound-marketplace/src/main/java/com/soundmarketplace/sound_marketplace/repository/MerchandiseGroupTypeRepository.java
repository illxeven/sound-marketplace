package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.MerchandiseGroupType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MerchandiseGroupTypeRepository extends JpaRepository<MerchandiseGroupType, Long> {
    Optional<MerchandiseGroupType> findByNameIgnoreCase(String name);
}
