package com.soundmarketplace.config;

import lombok.Data;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for Stripe payment gateway integration.
 * Placeholders are defined here and can be overridden in application.properties or environment variables.
 */
@Component
@Data
public class StripeProperties {

    @Value("${stripe.secret.key:${stripe.api.key:YOUR_STRIPE_SECRET_KEY_PLACEHOLDER}}")
    private String secretKey;

    public String getApiKey() {
        return secretKey;
    }

    public void setApiKey(String key) {
        this.secretKey = key;
    }

    @Value("${stripe.publishable.key:YOUR_STRIPE_PUBLISHABLE_KEY_PLACEHOLDER}")
    private String publishableKey;

    @Value("${stripe.webhook.secret:whsec_placeholder_webhook_secret}")
    private String webhookSecret;

    @Value("${stripe.currency:usd}")
    private String defaultCurrency;
}
