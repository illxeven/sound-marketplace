package com.soundmarketplace.sound_marketplace.repository;

import com.soundmarketplace.sound_marketplace.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Integer> {
    List<EmailLog> findByRecipientEmailOrderBySentAtDesc(String recipientEmail);
    List<EmailLog> findByUserIdOrderBySentAtDesc(Integer userId);
}
