package com.soundmarketplace.sound_marketplace.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracks")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Track {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = true)
    private UserAccount artist;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "album_id", nullable = true)
    private Album album;

    @Column(nullable = false)
    private String title;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "genre_id")
    private Genre genre;

    @Column(name = "is_promoted")
    @Builder.Default
    private Boolean isPromoted = false;

    @Column(name = "artist_name", length = 200)
    private String artistName;

    @Column(name = "features", length = 1000)
    private String features;

    @Column(name = "price")
    @Builder.Default
    private BigDecimal price = BigDecimal.ZERO;

    @Lob
    @Column(name = "cover_image_blob", columnDefinition = "LONGBLOB")
    private byte[] coverImageBlob;

    @Column(name = "video_file_path", length = 500)
    private String videoFilePath;

    @Lob
    @Column(name = "video_blob", columnDefinition = "LONGBLOB")
    private byte[] videoBlob;

    @Column(name = "media_type", length = 50)
    @Builder.Default
    private String mediaType = "AUDIO";

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
