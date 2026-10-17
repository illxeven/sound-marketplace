package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DocumentTypeRepository extends JpaRepository<DocumentType, Long> {
    Optional<DocumentType> findByNameIgnoreCase(String name);
}
