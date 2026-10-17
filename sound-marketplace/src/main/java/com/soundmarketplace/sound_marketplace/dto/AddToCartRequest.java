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
public class AddToCartRequest {
    @NotNull(message = "User ID is required")
    private Integer userId;

    private Integer merchandiseId;
    private Integer trackId;

    @Builder.Default
    private Integer quantity = 1;
}
