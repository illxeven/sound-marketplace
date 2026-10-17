package com.soundmarketplace.sound_marketplace.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConsentRequest {
    private Long userId;

    @NotNull(message = "Document ID is required")
    private Long documentId;

    private String ipAddress;
}
