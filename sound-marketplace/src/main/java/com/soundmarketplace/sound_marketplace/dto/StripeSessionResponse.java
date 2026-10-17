package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StripeSessionResponse {
    private String sessionId;
    private String paymentIntentId;
    private String clientSecret;
    private String checkoutUrl;
    private String status;
    private BigDecimal amount;
    private String currency;
    private Map<String, String> metadata;
}
