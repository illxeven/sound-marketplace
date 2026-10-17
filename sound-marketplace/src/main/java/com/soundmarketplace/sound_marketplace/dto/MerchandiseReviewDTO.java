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
public class MerchandiseReviewDTO {
    private Integer id;
    private Integer merchandiseId;
    private Integer userId;
    private String userEmail;
    private String userDisplayName;
    private Integer rating;
    private String comment;
    private LocalDateTime createdAt;
}
