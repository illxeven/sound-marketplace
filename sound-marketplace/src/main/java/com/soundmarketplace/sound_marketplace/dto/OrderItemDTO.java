package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemDTO {
    private Long id;
    private String itemType;
    private Integer trackId;
    private Integer albumId;
    private Integer merchandiseId;
    private String title;
    private Long unitPriceCents;
    private Integer quantity;
}
