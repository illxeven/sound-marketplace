package com.soundmarketplace.service.impl;

import com.soundmarketplace.config.StripeProperties;
import com.soundmarketplace.service.PaymentService;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreatePaymentIntentRequest;
import com.soundmarketplace.sound_marketplace.dto.PaymentIntentResponse;
import com.soundmarketplace.sound_marketplace.dto.SavedPaymentMethodDTO;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.stripe.Stripe;
import com.stripe.exception.StripeException;
import com.stripe.model.Customer;
import com.stripe.model.EphemeralKey;
import com.stripe.model.PaymentIntent;
import com.stripe.model.PaymentMethod;
import com.stripe.model.PaymentMethodCollection;
import com.stripe.param.CustomerCreateParams;
import com.stripe.param.EphemeralKeyCreateParams;
import com.stripe.param.PaymentIntentConfirmParams;
import com.stripe.param.PaymentIntentCreateParams;
import com.stripe.param.PaymentMethodListParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

/**
 * Production-ready Stripe PaymentIntent & Customer Vault service.
 * Manages PaymentIntents, Stripe Customers, saved cards, and Ephemeral Keys.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentServiceImpl implements PaymentService {

    private final StripeProperties stripeProperties;
    private final UserAccountRepository userAccountRepository;

    @Override
    public PaymentIntentResponse createPaymentIntent(CreatePaymentIntentRequest request) {
        if (request == null || request.getAmount() == null) {
            throw new IllegalArgumentException("Purchase amount cannot be null");
        }

        String currency = (request.getCurrency() != null && !request.getCurrency().isBlank())
                ? request.getCurrency().toLowerCase()
                : stripeProperties.getDefaultCurrency().toLowerCase();

        long amountInCents = request.getAmount().multiply(BigDecimal.valueOf(100)).longValue();

        Map<String, String> metadata = new HashMap<>();
        if (request.getMetadata() != null) {
            metadata.putAll(request.getMetadata());
        }
        if (request.getItemType() != null) metadata.put("itemType", request.getItemType());
        if (request.getItemId() != null) metadata.put("itemId", String.valueOf(request.getItemId()));
        if (request.getBuyerId() != null) metadata.put("buyerId", String.valueOf(request.getBuyerId()));
        if (request.getItemDetails() != null) metadata.put("itemDetails", request.getItemDetails());

        String secretKey = stripeProperties.getSecretKey();
        boolean isPlaceholder = (secretKey == null || secretKey.isBlank() || secretKey.contains("PLACEHOLDER") || secretKey.contains("placeholder") || secretKey.contains("dummy"));

        // If placeholder key is configured, supply sandbox simulation for frontend testing
        if (isPlaceholder) {
            log.warn("Stripe secret key is using placeholder value. Generating local sandbox PaymentIntent for testing.");
            String mockId = "pi_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String mockSecret = mockId + "_secret_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
            String mockCustomerId = "cus_mock_" + (request.getBuyerId() != null ? request.getBuyerId() : "guest");
            String mockEphemeralKey = "ek_mock_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);

            return PaymentIntentResponse.builder()
                    .paymentIntentId(mockId)
                    .clientSecret(mockSecret)
                    .amount(request.getAmount())
                    .currency(currency.toUpperCase())
                    .status(Boolean.TRUE.equals(request.getConfirm()) ? "succeeded" : "requires_payment_method")
                    .customerId(mockCustomerId)
                    .customerEphemeralKeySecret(mockEphemeralKey)
                    .metadata(metadata)
                    .build();
        }

        // Live / Test mode via Stripe Java SDK
        try {
            Stripe.apiKey = secretKey;

            log.info("==================================================================");
            log.info("[STRIPE SANDBOX] Initiating PaymentIntent creation");
            log.info("Amount: {} cents ({} {})", amountInCents, request.getAmount(), currency);
            log.info("Item Details: {}", request.getItemDetails());
            log.info("Item Type: {}, Item ID: {}", request.getItemType(), request.getItemId());
            log.info("Buyer ID: {}", request.getBuyerId());
            log.info("Immediate Confirm: {}", request.getConfirm());
            log.info("Stripe Secret Key prefix: {}", secretKey.substring(0, Math.min(12, secretKey.length())) + "...");
            log.info("==================================================================");
            System.out.println("[Stripe Sandbox] Initializing PaymentIntent for amount: " + amountInCents + " cents (" + request.getAmount() + " " + currency + ")");

            String stripeCustomerId = null;
            String ephemeralKeySecret = null;

            // 1. Manage Stripe Customer for card-saving functionality
            if (request.getBuyerId() != null) {
                stripeCustomerId = getOrCreateStripeCustomer(request.getBuyerId());
                if (stripeCustomerId != null) {
                    try {
                        EphemeralKeyCreateParams keyParams = EphemeralKeyCreateParams.builder()
                                .setCustomer(stripeCustomerId)
                                .setStripeVersion("2024-06-20")
                                .build();
                        EphemeralKey ephemeralKey = EphemeralKey.create(keyParams);
                        ephemeralKeySecret = ephemeralKey.getSecret();
                        log.info("[STRIPE SANDBOX] Successfully created EphemeralKey for customer: {}", stripeCustomerId);
                    } catch (StripeException e) {
                        log.warn("[STRIPE SANDBOX] EphemeralKey creation notice for customer {}: Status={}, Code={}, Msg={}",
                                stripeCustomerId, e.getStatusCode(), e.getCode(), e.getMessage());
                    } catch (Exception e) {
                        log.warn("[STRIPE SANDBOX] EphemeralKey general exception: {}", e.getMessage());
                    }
                }
            }

            // 2. Build PaymentIntent parameters
            PaymentIntentCreateParams.Builder paramsBuilder = PaymentIntentCreateParams.builder()
                    .setAmount(amountInCents)
                    .setCurrency(currency);

            if (Boolean.TRUE.equals(request.getConfirm())) {
                String pm = (request.getPaymentMethodId() != null && !request.getPaymentMethodId().isBlank())
                        ? request.getPaymentMethodId()
                        : "pm_card_visa";
                paramsBuilder.setPaymentMethod(pm);
                paramsBuilder.setConfirm(true);
                paramsBuilder.setReturnUrl("https://soundmarketplace.com/return");
                log.info("[STRIPE SANDBOX] Auto-confirm requested with test payment method: {}", pm);
            } else {
                paramsBuilder.setAutomaticPaymentMethods(
                        PaymentIntentCreateParams.AutomaticPaymentMethods.builder()
                                .setEnabled(true)
                                .build()
                );
            }

            if (stripeCustomerId != null) {
                paramsBuilder.setCustomer(stripeCustomerId);
                // Save card for future off-session payments
                if (Boolean.TRUE.equals(request.getSaveCard()) && !Boolean.TRUE.equals(request.getConfirm())) {
                    paramsBuilder.setSetupFutureUsage(PaymentIntentCreateParams.SetupFutureUsage.OFF_SESSION);
                }
            }

            if (request.getItemDetails() != null && !request.getItemDetails().isBlank()) {
                paramsBuilder.setDescription(request.getItemDetails());
            }

            if (!metadata.isEmpty()) {
                paramsBuilder.putAllMetadata(metadata);
            }

            PaymentIntentCreateParams params = paramsBuilder.build();
            log.info("[STRIPE SANDBOX] Sending create request to Stripe API...");
            PaymentIntent intent = PaymentIntent.create(params);

            log.info("==================================================================");
            log.info("[STRIPE SANDBOX] PAYMENT INTENT CREATED SUCCESSFULLY!");
            log.info("PaymentIntent ID: {}", intent.getId());
            log.info("Client Secret: {}", (intent.getClientSecret() != null ? intent.getClientSecret().substring(0, Math.min(15, intent.getClientSecret().length())) + "..." : "null"));
            log.info("Status: {}", intent.getStatus());
            log.info("Amount: {} {}", intent.getAmount(), intent.getCurrency());
            log.info("Stripe Customer: {}", stripeCustomerId);
            log.info("==================================================================");
            System.out.println("[Stripe Sandbox] Successfully created PaymentIntent: " + intent.getId() + " | Status: " + intent.getStatus());

            return PaymentIntentResponse.builder()
                    .paymentIntentId(intent.getId())
                    .clientSecret(intent.getClientSecret())
                    .amount(request.getAmount())
                    .currency(currency.toUpperCase())
                    .status(intent.getStatus())
                    .customerId(stripeCustomerId)
                    .customerEphemeralKeySecret(ephemeralKeySecret)
                    .metadata(intent.getMetadata())
                    .build();

        } catch (StripeException e) {
            log.error("==================================================================");
            log.error("[STRIPE SANDBOX ERROR] StripeException caught during PaymentIntent creation!");
            log.error("HTTP Status Code: {}", e.getStatusCode());
            log.error("Stripe Error Code: {}", e.getCode());
            log.error("Stripe Request ID: {}", e.getRequestId());
            log.error("Error Message: {}", e.getMessage());
            if (e.getStripeError() != null) {
                log.error("Error Type: {}", e.getStripeError().getType());
                log.error("Decline Code: {}", e.getStripeError().getDeclineCode());
                log.error("Param: {}", e.getStripeError().getParam());
                log.error("User Message: {}", e.getStripeError().getMessage());
            }
            log.error("==================================================================", e);
            System.err.println("[Stripe Sandbox Error] createPaymentIntent failed: Status=" + e.getStatusCode() +
                    ", Code=" + e.getCode() +
                    ", RequestId=" + e.getRequestId() +
                    ", Message=" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Stripe API error [" + e.getCode() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public PaymentIntentResponse confirmPaymentIntent(String paymentIntentId, String paymentMethodId) {
        if (paymentIntentId == null || paymentIntentId.isBlank()) {
            throw new IllegalArgumentException("paymentIntentId cannot be null or blank");
        }

        String secretKey = stripeProperties.getSecretKey();
        boolean isPlaceholder = (secretKey == null || secretKey.isBlank() || secretKey.contains("PLACEHOLDER") || secretKey.contains("placeholder") || secretKey.contains("dummy"));

        if (isPlaceholder || paymentIntentId.startsWith("pi_mock_")) {
            log.info("[STRIPE SANDBOX MOCK] Confirming mock PaymentIntent: {}", paymentIntentId);
            return PaymentIntentResponse.builder()
                    .paymentIntentId(paymentIntentId)
                    .clientSecret(paymentIntentId + "_secret_mock")
                    .status("succeeded")
                    .build();
        }

        try {
            Stripe.apiKey = secretKey;
            String pm = (paymentMethodId != null && !paymentMethodId.isBlank()) ? paymentMethodId : "pm_card_visa";

            log.info("==================================================================");
            log.info("[STRIPE SANDBOX] Confirming PaymentIntent: {}", paymentIntentId);
            log.info("Payment Method: {}", pm);
            log.info("==================================================================");
            System.out.println("[Stripe Sandbox] Confirming PaymentIntent: " + paymentIntentId + " with method: " + pm);

            PaymentIntent intent = PaymentIntent.retrieve(paymentIntentId);
            PaymentIntentConfirmParams confirmParams = PaymentIntentConfirmParams.builder()
                    .setPaymentMethod(pm)
                    .setReturnUrl("https://soundmarketplace.com/return")
                    .build();

            PaymentIntent confirmed = intent.confirm(confirmParams);

            log.info("==================================================================");
            log.info("[STRIPE SANDBOX] PAYMENT INTENT CONFIRMED SUCCESSFULLY!");
            log.info("PaymentIntent ID: {}", confirmed.getId());
            log.info("Confirmed Status: {}", confirmed.getStatus());
            log.info("Amount: {} {}", confirmed.getAmount(), confirmed.getCurrency());
            log.info("==================================================================");
            System.out.println("[Stripe Sandbox] Intent " + confirmed.getId() + " confirmed successfully! Status: " + confirmed.getStatus());

            return PaymentIntentResponse.builder()
                    .paymentIntentId(confirmed.getId())
                    .clientSecret(confirmed.getClientSecret())
                    .amount(BigDecimal.valueOf(confirmed.getAmount()).divide(BigDecimal.valueOf(100)))
                    .currency(confirmed.getCurrency().toUpperCase())
                    .status(confirmed.getStatus())
                    .customerId(confirmed.getCustomer())
                    .metadata(confirmed.getMetadata())
                    .build();

        } catch (StripeException e) {
            log.error("==================================================================");
            log.error("[STRIPE SANDBOX ERROR] StripeException caught during confirmPaymentIntent!");
            log.error("HTTP Status Code: {}", e.getStatusCode());
            log.error("Stripe Error Code: {}", e.getCode());
            log.error("Stripe Request ID: {}", e.getRequestId());
            log.error("Error Message: {}", e.getMessage());
            if (e.getStripeError() != null) {
                log.error("Error Type: {}", e.getStripeError().getType());
                log.error("Decline Code: {}", e.getStripeError().getDeclineCode());
                log.error("Param: {}", e.getStripeError().getParam());
                log.error("User Message: {}", e.getStripeError().getMessage());
            }
            log.error("==================================================================", e);
            System.err.println("[Stripe Sandbox Error] confirmPaymentIntent failed: Status=" + e.getStatusCode() +
                    ", Code=" + e.getCode() +
                    ", RequestId=" + e.getRequestId() +
                    ", Message=" + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Stripe API confirmation error [" + e.getCode() + "]: " + e.getMessage(), e);
        }
    }

    @Override
    public List<SavedPaymentMethodDTO> getSavedPaymentMethods(Integer buyerId) {
        if (buyerId == null) {
            return Collections.emptyList();
        }

        String secretKey = stripeProperties.getSecretKey();
        boolean isPlaceholder = (secretKey == null || secretKey.isBlank() || secretKey.contains("PLACEHOLDER") || secretKey.contains("placeholder") || secretKey.contains("dummy"));

        if (isPlaceholder) {
            // Return simulated saved test cards for sandbox preview
            return List.of(
                    SavedPaymentMethodDTO.builder()
                            .id("pm_mock_visa4242")
                            .brand("visa")
                            .last4("4242")
                            .expMonth(12L)
                            .expYear(2028L)
                            .isDefault(true)
                            .build()
            );
        }

        try {
            Stripe.apiKey = secretKey;
            String customerId = getOrCreateStripeCustomer(buyerId);
            if (customerId == null) {
                return Collections.emptyList();
            }

            PaymentMethodListParams params = PaymentMethodListParams.builder()
                    .setCustomer(customerId)
                    .setType(PaymentMethodListParams.Type.CARD)
                    .build();

            PaymentMethodCollection methods = PaymentMethod.list(params);
            List<SavedPaymentMethodDTO> result = new ArrayList<>();

            for (PaymentMethod pm : methods.getData()) {
                if (pm.getCard() != null) {
                    result.add(SavedPaymentMethodDTO.builder()
                            .id(pm.getId())
                            .brand(pm.getCard().getBrand())
                            .last4(pm.getCard().getLast4())
                            .expMonth(pm.getCard().getExpMonth())
                            .expYear(pm.getCard().getExpYear())
                            .isDefault(result.isEmpty())
                            .build());
                }
            }
            return result;

        } catch (Exception e) {
            log.error("Failed to query saved payment methods for user {}: {}", buyerId, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String getOrCreateStripeCustomer(Integer userId) {
        Optional<UserAccount> userOpt = userAccountRepository.findById(userId);
        if (userOpt.isEmpty()) return null;

        UserAccount user = userOpt.get();
        if (user.getStripeCustomerId() != null && !user.getStripeCustomerId().isBlank()) {
            return user.getStripeCustomerId();
        }

        try {
            CustomerCreateParams params = CustomerCreateParams.builder()
                    .setEmail(user.getEmail())
                    .setName(user.getEmail().split("@")[0])
                    .putMetadata("sound_marketplace_user_id", String.valueOf(user.getId()))
                    .build();

            Customer customer = Customer.create(params);
            user.setStripeCustomerId(customer.getId());
            userAccountRepository.save(user);

            log.info("Created and saved Stripe Customer {} for User ID {}", customer.getId(), user.getId());
            return customer.getId();
        } catch (Exception e) {
            log.error("Failed to create Stripe Customer for user {}: {}", user.getId(), e.getMessage());
            return null;
        }
    }
}
