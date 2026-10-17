package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTrackRequest {
    private Integer artistId;
    private Integer albumId;
    private String title;
    private String filePath;
    private String genre;
    private BigDecimal price;
    private String artistName;
    private String features;
    private Integer durationSeconds;
    private Boolean isPromoted;
    private String uploadSource;
    private Integer uploaderUserId;
    private byte[] coverImageBlob;
    private String videoFilePath;
    private byte[] videoBlob;
    private String mediaType;
}
