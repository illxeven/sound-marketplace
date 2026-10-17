package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateProfileRequest {
    private String displayName;
    private String bio;
    private String avatarUrl;
    private String country;
    private byte[] avatarImageBlob;
}
