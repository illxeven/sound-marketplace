package com.soundmarketplace.sound_marketplace.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Purchase request payload for creating a Stripe PaymentIntent.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatePaymentIntentRequest {

    /**
     * Purchase amount in decimal format (e.g., 9.99 for $9.99).
     */
    private BigDecimal amount;

    /**
     * Three-letter ISO currency code (default: 'usd').
     */
    private String currency;

    /**
     * Human-readable item or order description.
     */
    private String itemDetails;

    /**
     * Item category: 'track', 'merchandise', 'sound_kit', 'cart', 'album', etc.
     */
    private String itemType;

    /**
     * ID of the track, merchandise, or sound kit being purchased.
     */
    private Integer itemId;

    /**
     * ID of the authenticated user making the purchase.
     */
    private Integer buyerId;

    /**
     * Optional metadata key-value pairs stored on the Stripe PaymentIntent.
     */
    private Map<String, String> metadata;

    /**
     * Whether to save this card for future off-session purchases.
     */
    @Builder.Default
    private Boolean saveCard = true;

    /**
     * Optional payment method to immediately confirm in test/sandbox mode (e.g., 'pm_card_visa').
     */
    private String paymentMethodId;

    /**
     * Whether to immediately confirm the payment intent upon creation.
     */
    private Boolean confirm;
}
