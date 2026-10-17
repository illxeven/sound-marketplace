package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderDTO {
    private Long id;
    private Integer buyerId;
    private String status;
    private Long totalCents;
    private String currency;
    private String stripePaymentIntentId;
    private LocalDateTime createdAt;
    private LocalDateTime paidAt;
    private List<OrderItemDTO> items;
}
