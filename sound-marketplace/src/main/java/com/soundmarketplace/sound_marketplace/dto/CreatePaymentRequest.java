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
public class CreatePaymentRequest {
    private Integer buyerId;
    private Integer merchandiseId;
    private Integer trackId;
    private Integer albumId;
    private String itemType; // TRACK, BEAT, ALBUM, MERCHANDISE
    private String stripeChargeId;
    private BigDecimal amount;
    private String currency;
    private String status;
}
