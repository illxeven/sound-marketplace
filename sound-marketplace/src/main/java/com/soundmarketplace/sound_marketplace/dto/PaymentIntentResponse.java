package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Response payload returned to the frontend containing the Stripe PaymentIntent clientSecret.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PaymentIntentResponse {

    /**
     * Secret key passed to the frontend Flutter Stripe Payment Sheet to complete payment.
     */
    private String clientSecret;

    /**
     * Stripe PaymentIntent ID (e.g., 'pi_1234567890').
     */
    private String paymentIntentId;

    /**
     * Amount processed.
     */
    private BigDecimal amount;

    /**
     * Currency code (e.g., 'USD').
     */
    private String currency;

    /**
     * PaymentIntent status (e.g., 'requires_payment_method', 'succeeded').
     */
    private String status;

    /**
     * Associated order / item metadata.
     */
    private Map<String, String> metadata;

    /**
     * Stripe Customer ID (for saving card details for future purchases).
     */
    private String customerId;

    /**
     * Ephemeral key secret used by mobile Stripe SDK to access saved customer cards.
     */
    private String customerEphemeralKeySecret;
}
