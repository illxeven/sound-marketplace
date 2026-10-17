package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrackLikeId implements Serializable {

    @Column(name = "user_id")
    private Integer userId;

    @Column(name = "track_id")
    private Integer trackId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TrackLikeId that = (TrackLikeId) o;
        return Objects.equals(userId, that.userId) && Objects.equals(trackId, that.trackId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userId, trackId);
    }
}
