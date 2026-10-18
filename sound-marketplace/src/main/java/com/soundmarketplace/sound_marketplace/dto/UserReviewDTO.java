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
public class UserReviewDTO {
    private Integer id;
    private Integer reviewerId;
    private String reviewerDisplayName;
    private Integer targetUserId;
    private String targetUserDisplayName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
    private java.time.LocalDateTime updatedAt;
}
