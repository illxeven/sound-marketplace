package com.soundmarketplace.gateway;

import java.util.List;
import java.util.Map;

public interface StripeGateway {
    PaymentIntentResult createPaymentIntent(long amountCents, String currency, String customerId, Map<String, String> metadata, String idempotencyKey);
    CheckoutSessionResult createCheckoutSession(List<CheckoutLineItem> lineItems, String currency, String successUrl, String cancelUrl, Map<String, String> metadata);
    String createOrGetCustomer(String email, String name);
    String createEphemeralKey(String customerId);
}
