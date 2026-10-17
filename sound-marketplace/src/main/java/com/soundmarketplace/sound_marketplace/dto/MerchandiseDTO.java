package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MerchandiseDTO {
    private Integer id;
    private Integer merchandiseTypeId;
    private String merchandiseTypeName;
    private Long categoryId;
    private String categoryName;
    private Long groupTypeId;
    private String groupTypeName;
    private Integer sellerId;
    private String title;
    private BigDecimal price;
    private String description;
    private String coverImageUrl;
    private Integer imageCount;
    private String options;
    private Integer stockQuantity;
    private Integer quantity;
    private Double averageRating;
    private Long reviewCount;
    private LocalDateTime createdAt;
}
