package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_agreements")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserAgreement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    public Long getUserId() {
        return user != null && user.getId() != null ? user.getId().longValue() : null;
    }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "document_id", nullable = false)
    private LegalDocument document;

    @Column(name = "agreed_at")
    @Builder.Default
    private LocalDateTime agreedAt = LocalDateTime.now();

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;

    @PrePersist
    public void prePersist() {
        if (agreedAt == null) {
            agreedAt = LocalDateTime.now();
        }
    }
}
