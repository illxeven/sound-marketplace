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
public class CreateMerchandiseRequest {
    private Integer merchandiseTypeId;
    private Long categoryId;
    private Integer sellerId;
    private String title;
    private BigDecimal price;
    private String description;
    private String filePath;
    private byte[] imageBlob;
    private byte[] imageBlob2;
    private byte[] imageBlob3;
    private byte[] imageBlob4;
    private byte[] imageBlob5;
    private String options;
    private Integer stockQuantity;
    private Integer quantity;
}
