package com.soundmarketplace.gateway;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResult {
    private String paymentIntentId;
    private String clientSecret;
    private String customerId;
    private String ephemeralKey;
}
