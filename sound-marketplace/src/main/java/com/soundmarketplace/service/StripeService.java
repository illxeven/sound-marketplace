package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.StripeSessionResponse;

import java.math.BigDecimal;
import java.util.Map;

public interface StripeService {
    StripeSessionResponse createPaymentIntent(BigDecimal amount, String currency, String description, Map<String, String> metadata);
    StripeSessionResponse createCheckoutSession(BigDecimal amount, String currency, String itemName, String successUrl, String cancelUrl, Map<String, String> metadata);
}
