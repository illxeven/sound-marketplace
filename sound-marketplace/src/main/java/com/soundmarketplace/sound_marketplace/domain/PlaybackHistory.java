package com.soundmarketplace.sound_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "playback_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaybackHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = true)
    private UserAccount user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "played_at")
    @Builder.Default
    private LocalDateTime playedAt = LocalDateTime.now();

    public Integer getUserId() {
        return user != null ? user.getId() : null;
    }

    public Integer getTrackId() {
        return track != null ? track.getId() : null;
    }

    @PrePersist
    public void prePersist() {
        if (playedAt == null) {
            playedAt = LocalDateTime.now();
        }
    }
}
