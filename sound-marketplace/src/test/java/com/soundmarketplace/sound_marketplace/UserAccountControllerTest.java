package com.soundmarketplace.sound_marketplace;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.CreateUserRequest;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import com.soundmarketplace.sound_marketplace.repository.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class UserAccountControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private UserProfileRepository userProfileRepository;

    @BeforeEach
    void setUp() {
        for (String email : java.util.List.of("artist1@soundmarketplace.com", "buyer1@soundmarketplace.com", "producer@soundmarketplace.com")) {
            userAccountRepository.findByEmail(email).ifPresent(u -> {
                userProfileRepository.findByUserAccountId(u.getId()).ifPresent(userProfileRepository::delete);
                userAccountRepository.delete(u);
            });
        }
    }

    @Test
    @DisplayName("POST /api/users creates a new user and profile")
    void testCreateUser() throws Exception {
        CreateUserRequest request = CreateUserRequest.builder()
                .email("artist1@soundmarketplace.com")
                .password("securePassword123")
                .displayName("DJ Soundwave")
                .country("US")
                .termsAccepted(true)
                .build();

        mockMvc.perform(post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.email", is("artist1@soundmarketplace.com")))
                .andExpect(jsonPath("$.data.displayName", is("DJ Soundwave")))
                .andExpect(jsonPath("$.data.country", is("US")));
    }

    @Test
    @DisplayName("GET /api/users returns list of users")
    void testGetAllUsers() throws Exception {
        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .email("buyer1@soundmarketplace.com")
                .passwordHash("hashedPass")
                .isActive(true)
                .build());

        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data", hasSize(greaterThanOrEqualTo(1))));
    }

    @Test
    @DisplayName("PUT /api/users/{id}/profile updates user profile")
    void testUpdateProfile() throws Exception {
        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .email("producer@soundmarketplace.com")
                .passwordHash("hashedPass")
                .isActive(true)
                .build());

        Map<String, String> body = Map.of("displayName", "Master Producer", "country", "Canada");

        mockMvc.perform(put("/api/users/" + user.getId() + "/profile")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.displayName", is("Master Producer")))
                .andExpect(jsonPath("$.data.country", is("Canada")));
    }

    @Test
    @DisplayName("POST /api/users/forgot-password and /reset-password successfully resets password and verifies email")
    void testForgotPasswordAndResetPassword() throws Exception {
        UserAccount user = userAccountRepository.save(UserAccount.builder()
                .email("testrecovery@soundmarketplace.com")
                .passwordHash("oldHash123")
                .isActive(true)
                .isEmailVerified(false)
                .build());

        // Step 1: Request reset token
        var forgotResult = mockMvc.perform(post("/api/users/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("email", "testrecovery@soundmarketplace.com"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.data.token").exists())
                .andReturn();

        String responseBody = forgotResult.getResponse().getContentAsString();
        String token = objectMapper.readTree(responseBody).path("data").path("token").asText();
        org.junit.jupiter.api.Assertions.assertFalse(token.isBlank());

        // Step 2: Reset password using the returned token
        mockMvc.perform(post("/api/users/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "token", token,
                        "newPassword", "brandNewPassword456"
                ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success", is(true)))
                .andExpect(jsonPath("$.message", containsString("successfully")));

        // Step 3: Verify user in db has updated email verification and password
        UserAccount updated = userAccountRepository.findById(user.getId()).orElseThrow();
        org.junit.jupiter.api.Assertions.assertTrue(updated.getIsEmailVerified());
        org.junit.jupiter.api.Assertions.assertNotEquals("oldHash123", updated.getPasswordHash());
    }
}
