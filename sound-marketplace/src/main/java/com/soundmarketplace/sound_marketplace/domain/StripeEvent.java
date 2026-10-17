package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "stripe_events")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeEvent {

    @Id
    @Column(length = 255)
    private String id;

    @Column(nullable = false, length = 100)
    private String type;

    @Column(name = "received_at", nullable = false)
    @Builder.Default
    private LocalDateTime receivedAt = LocalDateTime.now();
}
