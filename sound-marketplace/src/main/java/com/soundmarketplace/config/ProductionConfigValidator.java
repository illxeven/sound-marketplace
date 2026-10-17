package com.soundmarketplace.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
@Profile("prod")
@RequiredArgsConstructor
@Slf4j
public class ProductionConfigValidator implements ApplicationRunner {

    private final Environment environment;

    private static final List<String> FORBIDDEN_SUBSTRINGS = List.of(
            "PLACEHOLDER",
            "placeholder",
            "YOUR_",
            "dummy"
    );

    private static final Map<String, String> REQUIRED_PROPERTIES = Map.of(
            "jwt.secret", "JWT_SECRET",
            "stream.secret", "STREAM_SECRET",
            "stripe.secret.key", "STRIPE_SECRET_KEY",
            "stripe.webhook.secret", "STRIPE_WEBHOOK_SECRET",
            "spring.datasource.url", "DB_URL",
            "spring.datasource.username", "DB_USER",
            "spring.datasource.password", "DB_PASSWORD"
    );

    @Override
    public void run(ApplicationArguments args) {
        log.info("Validating production configuration safety...");

        for (Map.Entry<String, String> entry : REQUIRED_PROPERTIES.entrySet()) {
            String propName = entry.getKey();
            String envName = entry.getValue();
            String value = environment.getProperty(propName);

            if (value == null || value.trim().isEmpty()) {
                throw new IllegalStateException(
                        String.format("Production configuration failure: required property '%s' (%s) is missing or empty.", propName, envName));
            }

            for (String forbidden : FORBIDDEN_SUBSTRINGS) {
                if (value.contains(forbidden)) {
                    throw new IllegalStateException(
                            String.format("Production configuration failure: property '%s' (%s) contains forbidden placeholder/dummy token '%s'.",
                                    propName, envName, forbidden));
                }
            }
        }

        log.info("Production configuration validated successfully. All required credentials are safe and non-dummy.");
    }
}
