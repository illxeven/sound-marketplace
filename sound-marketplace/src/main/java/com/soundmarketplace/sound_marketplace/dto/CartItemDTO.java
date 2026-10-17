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
public class CartItemDTO {
    private Integer id;
    private Integer cartId;
    private String itemType; // "MERCHANDISE" or "TRACK"
    
    // Merchandise info
    private Integer merchandiseId;
    private String merchandiseTitle;
    private String merchandiseCategory;
    private String merchandiseImageUrl;

    // Track info
    private Integer trackId;
    private String trackTitle;
    private String trackArtist;
    private String trackCoverUrl;

    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private LocalDateTime addedAt;
}
