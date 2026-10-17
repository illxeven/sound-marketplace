package com.soundmarketplace.sound_marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record MerchandiseSummaryDTO(
        Integer id,
        String title,
        Integer sellerId,
        String sellerName,
        BigDecimal price,
        Integer stockQuantity,
        String categoryName,
        String typeName,
        String coverUrl,
        Integer imageCount,
        LocalDateTime createdAt
) {}
