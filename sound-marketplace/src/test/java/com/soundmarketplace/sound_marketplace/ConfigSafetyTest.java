package com.soundmarketplace.sound_marketplace;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ConfigSafetyTest {

    @Test
    @DisplayName("In prod profile with a missing JWT_SECRET, context fails to start")
    void testProdProfileFailsStartupWhenJwtSecretMissing() {
        assertThatThrownBy(() -> {
            new SpringApplicationBuilder(SoundMarketplaceApplication.class)
                    .profiles("prod")
                    .properties(
                            "spring.datasource.url=jdbc:h2:mem:config_safety_test;DB_CLOSE_DELAY=-1;MODE=MySQL",
                            "spring.datasource.username=sa",
                            "spring.datasource.password=valid_password",
                            "stripe.secret.key=sk_live_valid_key",
                            "stripe.webhook.secret=whsec_valid_secret",
                            "stream.secret=valid_stream_secret_32bytes_min",
                            "jwt.secret="
                    )
                    .run();
        }).isNotNull();
    }

    @Test
    @DisplayName("In prod profile with dummy token, context fails to start")
    void testProdProfileFailsStartupWhenDummyTokenUsed() {
        assertThatThrownBy(() -> {
            new SpringApplicationBuilder(SoundMarketplaceApplication.class)
                    .profiles("prod")
                    .properties(
                            "spring.datasource.url=jdbc:h2:mem:config_safety_test2;DB_CLOSE_DELAY=-1;MODE=MySQL",
                            "spring.datasource.username=sa",
                            "spring.datasource.password=valid_password",
                            "stripe.secret.key=sk_test_dummy",
                            "stripe.webhook.secret=whsec_valid_secret",
                            "stream.secret=valid_stream_secret_32bytes_min",
                            "jwt.secret=valid_jwt_secret_32_bytes_long"
                    )
                    .run();
        }).isNotNull();
    }
}
