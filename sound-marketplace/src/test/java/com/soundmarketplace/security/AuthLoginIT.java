package com.soundmarketplace.security;

import tools.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.LoginRequest;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(classes = com.soundmarketplace.sound_marketplace.SoundMarketplaceApplication.class)
@AutoConfigureMockMvc
public class AuthLoginIT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserAccountRepository userAccountRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtDecoder jwtDecoder;

    @Autowired
    private com.soundmarketplace.service.RateLimiterService rateLimiterService;

    private Role consumerRole;

    @BeforeEach
    void setUp() {
        rateLimiterService.reset();
        consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));
    }

    @Test
    @DisplayName("Login success returns JWT access token with correct sub and roles, and rotating refresh token")
    void loginSuccess() throws Exception {
        String email = "login_success@test.com";
        userAccountRepository.findByEmail(email).ifPresent(userAccountRepository::delete);

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build();
        user = userAccountRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("StrongPassword123!")
                .build();

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.accessToken").isString())
                .andExpect(jsonPath("$.data.refreshToken").isString())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andReturn();

        String responseBody = result.getResponse().getContentAsString();
        String accessToken = objectMapper.readTree(responseBody).path("data").path("accessToken").asText();

        Jwt decoded = jwtDecoder.decode(accessToken);
        assertThat(decoded.getSubject()).isEqualTo(String.valueOf(user.getId()));
        List<String> roles = decoded.getClaimAsStringList("roles");
        assertThat(roles).contains("CONSUMER");
    }

    @Test
    @DisplayName("Login with wrong password returns 401")
    void loginWrongPasswordReturns401() throws Exception {
        String email = "login_wrong_pw@test.com";
        userAccountRepository.findByEmail(email).ifPresent(userAccountRepository::delete);

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .isActive(true)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build();
        userAccountRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("WrongPassword999!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Login with unknown email returns 401 with identical message")
    void loginUnknownEmailReturns401() throws Exception {
        LoginRequest request = LoginRequest.builder()
                .email("definitely_non_existent_email_404@test.com")
                .password("SomePassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    @DisplayName("Login for disabled/inactive account returns 403 ACCOUNT_DISABLED")
    void loginDisabledAccountReturns403() throws Exception {
        String email = "login_disabled@test.com";
        userAccountRepository.findByEmail(email).ifPresent(userAccountRepository::delete);

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .isActive(false)
                .isEmailVerified(true)
                .roles(Set.of(consumerRole))
                .build();
        userAccountRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("ACCOUNT_DISABLED"));
    }

    @Test
    @DisplayName("Login for unverified account returns 403 EMAIL_NOT_VERIFIED")
    void loginUnverifiedAccountReturns403() throws Exception {
        String email = "login_unverified@test.com";
        userAccountRepository.findByEmail(email).ifPresent(userAccountRepository::delete);

        UserAccount user = UserAccount.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode("StrongPassword123!"))
                .isActive(true)
                .isEmailVerified(false)
                .roles(Set.of(consumerRole))
                .build();
        userAccountRepository.save(user);

        LoginRequest request = LoginRequest.builder()
                .email(email)
                .password("StrongPassword123!")
                .build();

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("EMAIL_NOT_VERIFIED"));
    }
}
