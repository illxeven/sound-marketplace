package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AlbumDTO {
    private Integer id;
    private Integer artistId;
    private String artistEmail;
    private String title;
    private String coverArtUrl;
    private Boolean hasCoverImage;
    private String coverImageUrl;
    private LocalDateTime createdAt;
}
