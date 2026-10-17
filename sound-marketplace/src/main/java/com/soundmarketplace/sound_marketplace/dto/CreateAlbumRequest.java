package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateAlbumRequest {
    private Integer artistId;
    private String title;
    private String coverArtUrl;
    private byte[] coverImageBlob;
}
