package com.soundmarketplace.sound_marketplace.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "track_comments")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserAccount user;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "track_id", nullable = false)
    private Track track;

    @Column(name = "comment_text", nullable = false, length = 1000)
    private String commentText;

    @Column(name = "parent_comment_id")
    private Integer parentCommentId;

    @Column(name = "user_display_name")
    private String userDisplayName;

    @Column(name = "created_at")
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    public Integer getUserId() {
        return user != null ? user.getId() : null;
    }

    public Integer getTrackId() {
        return track != null ? track.getId() : null;
    }

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
