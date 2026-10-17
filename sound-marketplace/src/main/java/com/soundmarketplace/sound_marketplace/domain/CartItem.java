package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cart_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cart_id", nullable = false)
    private Cart cart;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "merchandise_id")
    private Merchandise merchandise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id")
    private Track track;

    @Column(nullable = false)
    @Builder.Default
    private Integer quantity = 1;

    @Column(name = "price_snapshot", nullable = false)
    @Builder.Default
    private BigDecimal priceSnapshot = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "added_at", updatable = false)
    private LocalDateTime addedAt;
}
