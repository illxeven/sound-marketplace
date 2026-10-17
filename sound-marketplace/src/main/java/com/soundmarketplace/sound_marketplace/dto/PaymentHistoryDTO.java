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
public class PaymentHistoryDTO {
    private Integer id;
    private Integer buyerId;
    private String buyerEmail;
    private Integer merchandiseId;
    private String merchandiseTitle;
    private String stripeChargeId;
    private BigDecimal amount;
    private String currency;
    private String status;
    private LocalDateTime createdAt;
}
