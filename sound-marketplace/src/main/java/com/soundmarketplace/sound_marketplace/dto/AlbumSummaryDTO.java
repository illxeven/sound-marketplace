package com.soundmarketplace.sound_marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AlbumSummaryDTO(
        Integer id,
        String title,
        Integer artistId,
        String artistName,
        BigDecimal price,
        String coverUrl,
        Long trackCount,
        LocalDateTime createdAt
) {}
