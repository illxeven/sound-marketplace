package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.dto.CreatePaymentIntentRequest;
import com.soundmarketplace.sound_marketplace.dto.PaymentIntentResponse;
import com.soundmarketplace.sound_marketplace.dto.SavedPaymentMethodDTO;

import java.util.List;

public interface PaymentService {

    /**
     * Creates a Stripe PaymentIntent for a purchase request and returns the clientSecret.
     *
     * @param request the purchase details including amount, currency, and item metadata
     * @return PaymentIntentResponse containing the clientSecret and paymentIntentId
     */
    PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request);

    /**
     * Retrieves saved payment methods (cards) for a registered user.
     *
     * @param buyerId ID of the user
     * @return list of saved payment cards
     */
    List<SavedPaymentMethodDTO> getSavedPaymentMethods(Integer buyerId);

    /**
     * Confirms an existing Stripe PaymentIntent.
     *
     * @param paymentIntentId Stripe PaymentIntent ID
     * @param paymentMethodId PaymentMethod to confirm with (e.g. pm_card_visa)
     * @return PaymentIntentResponse with confirmed status
     */
    PaymentIntentResponse confirmPaymentIntent(String paymentIntentId, String paymentMethodId);
}
