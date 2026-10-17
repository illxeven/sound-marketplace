package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.UserAgreement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAgreementRepository extends JpaRepository<UserAgreement, Long> {
    List<UserAgreement> findByUser_Id(Integer userId);
    boolean existsByUser_IdAndDocument_Id(Integer userId, Long documentId);
}
