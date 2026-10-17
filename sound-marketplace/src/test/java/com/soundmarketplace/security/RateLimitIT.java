package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.dto.LoginRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RateLimitIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private com.soundmarketplace.service.RateLimiterService rateLimiterService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        rateLimiterService.reset();
    }

    @Test
    @DisplayName("Rate limiting: 6th request within 1 minute returns 429 with Retry-After: 60")
    void loginRateLimiter() throws Exception {
        String testEmail = "rate_limit_" + System.currentTimeMillis() + "@test.com";
        LoginRequest request = LoginRequest.builder()
                .email(testEmail)
                .password("Password123!")
                .build();

        // 5 requests allowed (returning 401 because user does not exist)
        for (int i = 0; i < 5; i++) {
            mockMvc.perform(post("/api/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isUnauthorized());
        }

        // 6th request is rate-limited: 429 Too Many Requests
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isTooManyRequests())
                .andExpect(header().string("Retry-After", "60"));
    }
}
