package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "email_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "recipient_email", nullable = false)
    private String recipientEmail;

    @Column(name = "sender_email", nullable = false)
    private String senderEmail;

    @Column(nullable = false)
    private String subject;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String body;

    @Column(nullable = false, length = 50)
    @Builder.Default
    private String status = "SENT";

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "sent_at")
    @Builder.Default
    private LocalDateTime sentAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (sentAt == null) {
            sentAt = LocalDateTime.now();
        }
    }
}
