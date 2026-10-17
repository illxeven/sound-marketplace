package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "track_likes")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackLike {

    @EmbeddedId
    private TrackLikeId id;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("userId")
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("trackId")
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (id == null && user != null && track != null) {
            id = new TrackLikeId(user.getId(), track.getId());
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
