package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.LegalDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LegalDocumentRepository extends JpaRepository<LegalDocument, Long> {
    List<LegalDocument> findByIsActiveTrue();
    List<LegalDocument> findByDocumentTypes_NameIgnoreCaseAndIsActiveTrue(String name);
}
