package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_player_state")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPlayerState {

    @Id
    @Column(name = "user_id")
    private Integer userId;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private UserAccount user;

    @Column(name = "current_position")
    private Integer currentPosition;

    @Column(name = "position_ms", nullable = false)
    @Builder.Default
    private Integer positionMs = 0;

    @Column(nullable = false)
    @Builder.Default
    private Boolean shuffle = false;

    @Column(name = "repeat_mode", nullable = false, length = 8)
    @Builder.Default
    private String repeatMode = "OFF";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
