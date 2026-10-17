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
public class MarketplaceItemDTO {
    private Integer id;
    private String itemType; // SONG, BEAT, ALBUM, MERCH
    private String title;
    private String description;
    private BigDecimal price;
    private String currency;
    private Integer sellerId;
    private String sellerName;
    private String genre;
    private String coverArtUrl;
    private String options;
    private LocalDateTime createdAt;
}
