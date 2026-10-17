package com.soundmarketplace.gateway;

import com.soundmarketplace.config.StripeProperties;
import com.stripe.Stripe;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
import com.stripe.model.checkout.Session;
import com.stripe.net.RequestOptions;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.checkout.SessionCreateParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class StripeSdkGateway implements StripeGateway {

    private final StripeProperties stripeProperties;

    private void ensureApiKey() {
        if (Stripe.apiKey == null || Stripe.apiKey.isBlank()) {
            Stripe.apiKey = stripeProperties.getSecretKey();
        }
    }

    private boolean isDummyKey() {
        String key = stripeProperties.getSecretKey();
        return key == null || key.isBlank() || key.contains("dummy") || key.contains("PLACEHOLDER");
    }

    @Override
    public PaymentIntentResult createPaymentIntent(long amountCents, String currency, String customerId, Map<String, String> metadata, String idempotencyKey) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        try {
            ensureApiKey();
            if (isDummyKey()) {
                String piId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                String secret = piId + "_secret_" + UUID.randomUUID().toString().substring(0, 8);
                return PaymentIntentResult.builder()
                        .paymentIntentId(piId)
                        .clientSecret(secret)
                        .customerId(customerId != null ? customerId : "cus_mock_" + UUID.randomUUID().toString().substring(0, 8))
                        .ephemeralKey("ek_test_" + UUID.randomUUID().toString().substring(0, 8))
                        .build();
            }

            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountCents)
                    .setCurrency(currency.toLowerCase())
                    .setAutomaticPaymentMethods(PaymentIntentCreateParams.AutomaticPaymentMethods.builder().setEnabled(true).build())
                    .putAllMetadata(metadata != null ? metadata : Map.of());

            if (customerId != null && !customerId.isBlank()) {
                paramsBuilder.setCustomer(customerId);
            }

            RequestOptions options = RequestOptions.builder()
                    .setIdempotencyKey(idempotencyKey)
                    .build();

            PaymentIntent intent = PaymentIntent.create(paramsBuilder.build(), options);
            return PaymentIntentResult.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .customerId(intent.getCustomer())
                    .build();
        } catch (Exception e) {
            outcome = "error";
            log.error("Stripe paymentIntent creation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create Stripe payment intent: " + e.getMessage(), e);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=stripe op=paymentIntents.create durationMs={} outcome={}", durationMs, outcome);
        }
    }

    @Override
    public CheckoutSessionResult createCheckoutSession(List<CheckoutLineItem> lineItems, String currency, String successUrl, String cancelUrl, Map<String, String> metadata) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        try {
            ensureApiKey();
            if (isDummyKey()) {
                String sessionId = "cs_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
                String url = "https://checkout.stripe.com/pay/" + sessionId;
                return CheckoutSessionResult.builder()
                        .sessionId(sessionId)
                        .url(url)
                        .build();
            }

            SessionCreateParams.Builder sessionBuilder = SessionCreateParams.builder()
                    .setMode(SessionCreateParams.Mode.PAYMENT)
                    .setSuccessUrl(successUrl)
                    .setCancelUrl(cancelUrl)
                    .putAllMetadata(metadata != null ? metadata : Map.of());

            if (metadata != null && metadata.containsKey("orderId")) {
                sessionBuilder.setPaymentIntentData(
                        SessionCreateParams.PaymentIntentData.builder()
                                .putMetadata("orderId", metadata.get("orderId"))
                                .build()
                );
            }

            if (lineItems != null) {
                for (CheckoutLineItem item : lineItems) {
                    sessionBuilder.addLineItem(
                            SessionCreateParams.LineItem.builder()
                                    .setQuantity(item.getQuantity())
                                    .setPriceData(
                                            SessionCreateParams.LineItem.PriceData.builder()
                                                    .setCurrency(currency.toLowerCase())
                                                    .setUnitAmount(item.getUnitAmountCents())
                                                    .setProductData(
                                                            SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                                                    .setName(item.getName())
                                                                    .build()
                                                    )
                                                    .build()
                                    )
                                    .build()
                    );
                }
            }

            Session session = Session.create(sessionBuilder.build());
            return CheckoutSessionResult.builder()
                    .sessionId(session.getId())
                    .url(session.getUrl())
                    .build();
        } catch (Exception e) {
            outcome = "error";
            log.error("Stripe checkout session creation failed: {}", e.getMessage());
            throw new RuntimeException("Failed to create Stripe checkout session: " + e.getMessage(), e);
        } finally {
            long durationMs = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=stripe op=checkout.sessions.create durationMs={} outcome={}", durationMs, outcome);
        }
    }

    @Override
    public String createOrGetCustomer(String email, String name) {
        ensureApiKey();
        if (isDummyKey()) {
            return "cus_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 12);
        }
        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(email)
                    .setName(name)
                    .build();
            Customer customer = Customer.create(params);
            return customer.getId();
        } catch (Exception e) {
            log.warn("Failed to create Stripe customer, falling back: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public String createEphemeralKey(String customerId) {
        ensureApiKey();
        if (isDummyKey() || customerId == null) {
            return "ek_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
        try {
            EphemeralKeyCreateParams params = EphemeralKeyCreateParams.builder()
                    .setCustomer(customerId)
                    .setStripeVersion(Stripe.API_VERSION)
                    .build();
            EphemeralKey key = EphemeralKey.create(params);
            return key.getSecret();
        } catch (Exception e) {
            log.warn("Failed to create Stripe ephemeral key: {}", e.getMessage());
            return "ek_test_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        }
    }
}
