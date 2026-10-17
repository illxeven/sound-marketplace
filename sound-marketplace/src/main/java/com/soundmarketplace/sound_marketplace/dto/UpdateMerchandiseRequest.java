package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateMerchandiseRequest {
    private String title;
    private BigDecimal price;
    private String description;
    private Integer stockQuantity;
    private Integer quantity;
    private String options;
    private Long categoryId;
    private Integer merchandiseTypeId;
    private byte[] imageBlob;
    private byte[] imageBlob2;
    private byte[] imageBlob3;
    private byte[] imageBlob4;
    private byte[] imageBlob5;
}
