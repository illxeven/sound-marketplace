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
@Table(name = "albums")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Album {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "artist_id", nullable = false)
    private UserAccount artist;

    @Column(nullable = false)
    private String title;

    @Column(name = "cover_art_url")
    private String coverArtUrl;

    @Column(name = "price", nullable = false)
    @Builder.Default
    private BigDecimal price = BigDecimal.valueOf(9.99);

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Lob
    @Column(name = "cover_image_blob", columnDefinition = "LONGBLOB")
    private byte[] coverImageBlob;

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
