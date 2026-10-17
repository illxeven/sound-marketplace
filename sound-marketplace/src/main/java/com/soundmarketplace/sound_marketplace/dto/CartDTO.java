package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CartDTO {
    private Integer id;
    private Integer userId;
    @Builder.Default
    private List<CartItemDTO> items = new ArrayList<>();
    private Integer totalItems;
    private BigDecimal subtotal;
}
