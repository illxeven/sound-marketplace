package com.soundmarketplace.payment;

import com.soundmarketplace.security.JwtTestTokens;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RemovedEndpointsIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTestTokens jwtTestTokens;

    @ParameterizedTest
    @ValueSource(strings = {
            "/api/payments",
            "/api/payments/buy/track/1",
            "/api/payments/buy/merchandise/1",
            "/api/payments/buy/album/1",
            "/api/payments/confirm-intent",
            "/api/payments/checkout-session",
            "/api/payments/create-intent"
    })
    @DisplayName("Deleted payment endpoints return 404 Not Found or 405 Method Not Allowed")
    void testDeletedEndpointsReturn404Or405(String path) throws Exception {
        int status = mockMvc.perform(post(path)
                        .header("Authorization", jwtTestTokens.consumer(1))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn().getResponse().getStatus();

        assertThat(status)
                .withFailMessage("Expected 404 or 405 for removed endpoint %s, but got %d", path, status)
                .isIn(404, 405);
    }
}
