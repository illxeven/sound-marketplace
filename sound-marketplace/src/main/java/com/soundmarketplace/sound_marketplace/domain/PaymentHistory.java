package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payment_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "buyer_id", nullable = false)
    private UserAccount buyer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandise_id")
    private Merchandise merchandise;

    @Column(name = "stripe_charge_id", nullable = false)
    private String stripeChargeId;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    @Builder.Default
    private String currency = "USD";

    @Column(nullable = false)
    @Builder.Default
    private String status = "COMPLETED";

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
