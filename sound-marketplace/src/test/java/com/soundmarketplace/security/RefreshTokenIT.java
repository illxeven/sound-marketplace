package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.LoginRequest;
import com.soundmarketplace.sound_marketplace.dto.RefreshTokenRequest;
import com.soundmarketplace.sound_marketplace.repository.RefreshTokenRepository;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class RefreshTokenIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private com.soundmarketplace.service.RateLimiterService rateLimiterService;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        String email = "refresh_test_" + System.currentTimeMillis() + "_" + java.util.UUID.randomUUID().toString().substring(0, 8) + "@test.com";

        testUser = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());
    }

    private String loginAndGetRefreshToken() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email(testUser.getEmail())
                .password("StrongPassword123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();

        return objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
    }

    @Test
    @DisplayName("Rotating refresh token returns a new pair and old token is rotated")
    void refreshTokenRotation() throws Exception {
        String token1 = loginAndGetRefreshToken();
        assertThat(token1).isNotBlank();

        // Rotate token1 -> token2
        RefreshTokenRequest request = new RefreshTokenRequest(token1);
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andReturn();

        String token2 = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();
        assertThat(token2).isNotEqualTo(token1);
    }

    @Test
    @DisplayName("Reusing a rotated refresh token revokes the chain and returns 401")
    void reuseRotatedTokenRevokesChain() throws Exception {
        String token1 = loginAndGetRefreshToken();

        // First use: rotates token1 -> token2
        RefreshTokenRequest request1 = new RefreshTokenRequest(token1);
        MvcResult result = mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isOk())
                .andReturn();

        String token2 = objectMapper.readTree(result.getResponse().getContentAsString())
                .path("data").path("refreshToken").asText();

        // Second use of token1: should be rejected with 401 and revoke chain
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isUnauthorized());

        // Token2 should now also be revoked
        RefreshTokenRequest request2 = new RefreshTokenRequest(token2);
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Logout revokes the refresh token")
    void logoutRevokesToken() throws Exception {
        String token = loginAndGetRefreshToken();

        RefreshTokenRequest request = new RefreshTokenRequest(token);
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // Subsequent refresh attempt must fail with 401
        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }
}
