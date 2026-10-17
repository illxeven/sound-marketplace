package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayUrlResponse {
    private Integer trackId;
    private String url;
    private Long expiresAt;
    private Boolean isPreview;
}
