package com.soundmarketplace.config;

import com.stripe.Stripe;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Stripe SDK configuration and lifecycle initializer.
 * Sets the global Stripe API secret key upon application startup.
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class StripeConfig {

    private final StripeProperties stripeProperties;

    @PostConstruct
    public void initStripe() {
        String key = stripeProperties.getSecretKey();
        if (key != null && !key.isBlank() && !key.contains("PLACEHOLDER")) {
            Stripe.apiKey = key;
            log.info("Initialized Stripe SDK with configured secret key (prefix: {})",
                    key.length() > 7 ? key.substring(0, 7) + "..." : "configured");
        } else {
            // Set whatever key is configured or default placeholder
            Stripe.apiKey = key != null ? key : "YOUR_STRIPE_SECRET_KEY_PLACEHOLDER";
            log.warn("Stripe secret key is set to a placeholder ('{}'). Real live/test transactions require a valid 'sk_test_...' key in application.properties.", key);
        }
    }
}
