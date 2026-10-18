package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.PasswordResetToken;
import com.soundmarketplace.sound_marketplace.domain.RefreshToken;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.ForgotPasswordRequest;
import com.soundmarketplace.sound_marketplace.dto.ResetPasswordRequest;
import com.soundmarketplace.sound_marketplace.repository.PasswordResetTokenRepository;
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

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class PasswordResetIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private RefreshTokenRepository refreshTokenRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private UserAccount testUser;

    @BeforeEach
    void setUp() {
        passwordResetTokenRepository.deleteAll();
        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        String email = "reset_test_" + System.currentTimeMillis() + "@test.com";
        testUser = userAccountRepository.save(UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("OldPassword123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build());
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("NTF-03: Forgot-password returns 202 without token in body, identical for known and unknown email")
    void forgotPasswordReturns202NoToken() throws Exception {
        ForgotPasswordRequest knownReq = new ForgotPasswordRequest(testUser.getEmail());
        MvcResult knownRes = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(knownReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        ForgotPasswordRequest unknownReq = new ForgotPasswordRequest("nonexistent_" + System.currentTimeMillis() + "@test.com");
        MvcResult unknownRes = mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(unknownReq)))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist())
                .andReturn();

        assertThat(knownRes.getResponse().getContentAsString())
                .isEqualTo(unknownRes.getResponse().getContentAsString());
    }

    @Test
    @DisplayName("NTF-03: Token hash is stored in DB; single-use only; refresh tokens revoked on reset")
    void resetPasswordLifecycle() throws Exception {
        // Real codes are 8 characters and hashed in upper case; users may type them in lower case.
        String rawToken = "k7m2p9qx";
        String tokenHash = sha256Hex(rawToken.toUpperCase());

        PasswordResetToken resetToken = passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userAccount(testUser)
                .tokenHash(tokenHash)
                .expiryDate(LocalDateTime.now().plusMinutes(30))
                .used(false)
                .build());

        // Assign a refresh token to user
        RefreshToken refreshToken = refreshTokenRepository.save(RefreshToken.builder()
                .user(testUser)
                .tokenHash("some_refresh_hash")
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build());

        // 1. First reset succeeds
        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 2. Token is marked used
        entityManager.clear();
        PasswordResetToken updatedToken = passwordResetTokenRepository.findById(resetToken.getId()).orElseThrow();
        assertThat(updatedToken.getUsed()).isTrue();
        assertThat(updatedToken.getUsedAt()).isNotNull();
        assertThat(updatedToken.getToken()).as("no plaintext code kept").isNull();

        // 3. User refresh tokens revoked
        List<RefreshToken> userRefreshTokens = refreshTokenRepository.findByUser_Id(testUser.getId());
        for (RefreshToken rt : userRefreshTokens) {
            assertThat(rt.getRevokedAt()).isNotNull();
        }

        // 4. Using token a second time gives 400
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @DisplayName("NTF-03: Expired token is rejected with 400")
    void expiredTokenRejected() throws Exception {
        String rawToken = "EXP2R3D4";
        String tokenHash = sha256Hex(rawToken);

        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .userAccount(testUser)
                .tokenHash(tokenHash)
                .expiryDate(LocalDateTime.now().minusMinutes(5)) // expired
                .used(false)
                .build());

        ResetPasswordRequest resetReq = ResetPasswordRequest.builder()
                .token(rawToken)
                .newPassword("BrandNewPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }
}
