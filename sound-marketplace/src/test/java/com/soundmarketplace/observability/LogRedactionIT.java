package com.soundmarketplace.observability;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.dto.ForgotPasswordRequest;
import com.soundmarketplace.sound_marketplace.repository.PasswordResetTokenRepository;
import com.soundmarketplace.sound_marketplace.repository.RoleRepository;
import com.soundmarketplace.sound_marketplace.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LogRedactionIT {

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

    private ListAppender<ILoggingEvent> listAppender;
    private Logger targetLogger;

    @BeforeEach
    void setUp() {
        targetLogger = (Logger) LoggerFactory.getLogger("com.soundmarketplace");
        listAppender = new ListAppender<>();
        listAppender.start();
        targetLogger.addAppender(listAppender);

        Role consumerRole = roleRepository.findByRoleNameIgnoreCase("consumer")
                .orElseGet(() -> roleRepository.save(Role.builder().roleName("consumer").build()));

        if (userAccountRepository.findByEmail("redaction_test@soundmarketplace.com").isEmpty()) {
            userAccountRepository.save(UserAccount.builder()
                    .email("redaction_test@soundmarketplace.com")
                    .passwordHash("secretSuperPassword123")
                    .isActive(true)
                    .roles(Set.of(consumerRole))
                    .build());
        }
    }

    @AfterEach
    void tearDown() {
        if (targetLogger != null && listAppender != null) {
            targetLogger.detachAppender(listAppender);
            listAppender.stop();
        }
    }

    @Test
    @DisplayName("Verify forgot-password flow does not log password, raw token, or user email")
    void testForgotPasswordLogsDoNotLeakSecretsOrEmail() throws Exception {
        String testEmail = "redaction_test@soundmarketplace.com";
        ForgotPasswordRequest request = ForgotPasswordRequest.builder()
                .email(testEmail)
                .build();

        mockMvc.perform(post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        assertThat(listAppender.list).isNotEmpty();

        for (ILoggingEvent event : listAppender.list) {
            String message = event.getFormattedMessage();
            assertThat(message)
                    .withFailMessage("Log line leaked email: " + message)
                    .doesNotContain(testEmail);
            assertThat(message)
                    .withFailMessage("Log line leaked password: " + message)
                    .doesNotContain("secretSuperPassword123");
        }
    }
}
