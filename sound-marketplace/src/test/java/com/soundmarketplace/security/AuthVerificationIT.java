package com.soundmarketplace.security;

import com.soundmarketplace.sound_marketplace.domain.PasswordResetToken;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.PasswordResetTokenRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.soundmarketplace.service.UserAccountService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Email verification, resend and password-recovery behaviour added in the V7 change set. */
@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class AuthVerificationIT {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserAccountRepository userAccountRepository;
    @Autowired private PasswordResetTokenRepository passwordResetTokenRepository;
    @Autowired private UserAccountService userAccountService;
    @Autowired private PasswordEncoder passwordEncoder;

    private static String sha256Hex(String s) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(s.getBytes(StandardCharsets.UTF_8)));
    }

    private UserAccount unverifiedUser(String code, LocalDateTime expires) throws Exception {
        return userAccountRepository.save(UserAccount.builder()
                .email("verify_" + System.nanoTime() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password12345"))
                .isActive(true)
                .isEmailVerified(false)
                .verificationTokenHash(sha256Hex(code))
                .verificationExpiresAt(expires)
                .build());
    }

    @BeforeEach
    void clean() {
        passwordResetTokenRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /api/auth/verify activates the account immediately and records verified_at")
    void verifyShortLinkActivates() throws Exception {
        UserAccount u = unverifiedUser("ABCD2345", LocalDateTime.now().plusHours(1));

        mockMvc.perform(get("/api/auth/verify").param("token", "abcd2345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        UserAccount after = userAccountRepository.findById(u.getId()).orElseThrow();
        assertThat(after.getIsEmailVerified()).isTrue();
        assertThat(after.getVerifiedAt()).isNotNull();
        assertThat(after.getVerificationTokenHash()).isNull();
    }

    @Test
    @DisplayName("Old /api/auth/verify-email links keep working")
    void legacyVerifyEmailPath() throws Exception {
        unverifiedUser("WXYZ6789", LocalDateTime.now().plusHours(1));
        mockMvc.perform(get("/api/auth/verify-email").param("token", "WXYZ6789"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Expired link opened in a browser shows a friendly HTML error, not JSON")
    void expiredLinkHtmlError() throws Exception {
        unverifiedUser("EXPD2345", LocalDateTime.now().minusMinutes(1));
        mockMvc.perform(get("/api/auth/verify").param("token", "EXPD2345").accept(MediaType.TEXT_HTML))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_HTML))
                .andExpect(content().string(containsString("Resend code")));
    }

    @Test
    @DisplayName("Reset page escapes the token from the URL (no reflected script)")
    void resetPageEscapesToken() throws Exception {
        mockMvc.perform(get("/api/auth/reset-password").param("token", "<script>alert(1)</script>"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("<script>alert(1)</script>"))))
                .andExpect(content().string(containsString("&lt;script&gt;")));
    }

    @Test
    @DisplayName("Forgot-password: identical generic response for known and unknown emails")
    void forgotPasswordIsGeneric() throws Exception {
        UserAccount u = unverifiedUser("GENR2345", LocalDateTime.now().plusHours(1));

        String known = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + u.getEmail() + "\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();
        String unknown = mockMvc.perform(post("/api/auth/forgot-password").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"nobody_" + System.nanoTime() + "@test.com\"}"))
                .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();

        assertThat(known.replaceAll("\"requestId\":\"[^\"]*\"", ""))
                .isEqualTo(unknown.replaceAll("\"requestId\":\"[^\"]*\"", ""));
    }

    @Test
    @DisplayName("Reset request stores only the hash, records IP, and burns earlier codes")
    void resetRequestStoresHashOnly() {
        UserAccount u = userAccountRepository.save(UserAccount.builder()
                .email("hashonly_" + System.nanoTime() + "@test.com")
                .passwordHash(passwordEncoder.encode("Password12345"))
                .isActive(true).isEmailVerified(true).build());

        String first = userAccountService.requestPasswordReset(u.getEmail(), "203.0.113.5");
        String second = userAccountService.requestPasswordReset(u.getEmail(), "203.0.113.5");

        List<PasswordResetToken> tokens = passwordResetTokenRepository.findByUserAccountId(u.getId());
        assertThat(tokens).hasSize(2);
        assertThat(tokens).allSatisfy(t -> assertThat(t.getToken()).as("no plaintext").isNull());
        assertThat(tokens).allSatisfy(t -> assertThat(t.getRequestedIp()).isEqualTo("203.0.113.5"));
        assertThat(tokens.stream().filter(t -> !t.getUsed()).count()).as("only newest is live").isEqualTo(1);
        assertThat(first).isNotEqualTo(second);
    }

    @Test
    @DisplayName("Resend: generic response, and throttled to one email per 60 s")
    void resendThrottled() throws Exception {
        UserAccount u = unverifiedUser("RSND2345", LocalDateTime.now().plusHours(1));
        u.setVerificationSentAt(LocalDateTime.now());
        userAccountRepository.save(u);
        String hashBefore = u.getVerificationTokenHash();

        mockMvc.perform(post("/api/auth/resend-verification").param("email", u.getEmail()))
                .andExpect(status().isOk());

        assertThat(userAccountRepository.findById(u.getId()).orElseThrow().getVerificationTokenHash())
                .as("still inside cooldown, code not replaced").isEqualTo(hashBefore);

        mockMvc.perform(post("/api/auth/resend-verification").param("email", "nobody_" + System.nanoTime() + "@test.com"))
                .andExpect(status().isOk());
    }
}
