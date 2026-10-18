package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddToCartRequest {
    /** Set by the server from the signed-in user; never trusted from the request body. */
    private Integer userId;

    private Integer merchandiseId;
    private Integer trackId;

    @Builder.Default
    private Integer quantity = 1;
}
