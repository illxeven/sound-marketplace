package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackDTO {
    private Integer id;
    private Integer artistId;
    private String artistName;
    private String features;
    private Integer albumId;
    private String albumTitle;
    private String title;
    private String filePath;
    private Integer durationSeconds;
    private String genre;
    private Boolean isPromoted;
    private BigDecimal price;
    private Boolean hasCoverImage;
    private String coverImageUrl;
    private byte[] coverImageBlob;
    private Long likesCount;
    private Long playsCount;
    private String videoFilePath;
    private byte[] videoBlob;
    private String mediaType;
    private LocalDateTime createdAt;
}
