package com.soundmarketplace.sound_marketplace.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TrackSummaryDTO(
        Integer id,
        String title,
        Integer artistId,
        String artistName,
        Integer albumId,
        String albumTitle,
        String genre,
        Integer durationSeconds,
        BigDecimal price,
        Long playCount,
        Integer likeCount,
        String mediaType,
        String coverUrl,
        LocalDateTime createdAt
) {}
