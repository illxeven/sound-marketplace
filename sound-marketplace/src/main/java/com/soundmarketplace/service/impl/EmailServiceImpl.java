package com.soundmarketplace.service.impl;

import com.soundmarketplace.service.EmailService;
import com.soundmarketplace.sound_marketplace.domain.EmailLog;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import com.soundmarketplace.sound_marketplace.repository.EmailLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final EmailLogRepository emailLogRepository;
    private final com.soundmarketplace.gateway.MailGateway mailGateway;
    private final com.soundmarketplace.sound_marketplace.repository.UserAccountRepository userAccountRepository;
    private final com.soundmarketplace.sound_marketplace.repository.UserProfileRepository userProfileRepository;

    @Value("${spring.mail.username:${app.mail.sender:noreply@soundmarketplace.com}}")
    private String senderEmail;

    @Value("${app.base.url:http://localhost:8080}")
    private String appBaseUrl;

    @Override
    public void sendWelcomeEmail(UserAccount user, String displayName) {
        String name = (displayName != null && !displayName.isBlank()) ? displayName : "Music Lover";
        String subject = "Welcome to Sound Marketplace!";
        String body = String.format(
                "Hello %s,\n\nWelcome to Sound Marketplace! Your account has been created successfully.\n" +
                "You can now discover exclusive tracks, buy beats and merchandise, and connect with artists.\n\n" +
                "The Sound Marketplace Team", name);

        sendEmail(user.getEmail(), user.getId(), subject, body);
    }

    @Override
    public void sendVerificationEmail(UserAccount user, String displayName, String token) {
        String name = (displayName != null && !displayName.isBlank()) ? displayName : "Music Lover";
        String subject = "Sound Marketplace - Verify Your Email Address";
        String body = String.format(
                "Hello %s,\n\n" +
                "Thank you for signing up for Sound Marketplace!\n" +
                "Please verify your email address to activate your account.\n\n" +
                "=========================================\n" +
                "YOUR VERIFICATION CODE: %s\n" +
                "=========================================\n\n" +
                "Enter this 8-character code in the Sound Marketplace app, or click the link below to verify instantly:\n\n" +
                "%s/api/auth/verify-email?token=%s\n\n" +
                "If you did not sign up for this account, you can safely ignore this email.\n\n" +
                "The Sound Marketplace Team", name, token, appBaseUrl, token);

        sendEmail(user.getEmail(), user.getId(), subject, body);
    }

    @Override
    public void sendVerificationEmail(Integer userId, String token) {
        userAccountRepository.findById(userId).ifPresent(user -> {
            String displayName = userProfileRepository.findByUserAccountId(userId)
                    .map(com.soundmarketplace.sound_marketplace.domain.UserProfile::getDisplayName)
                    .orElse("Music Lover");
            sendVerificationEmail(user, displayName, token);
        });
    }

    @Override
    public void sendPasswordResetEmail(String recipientEmail, Integer userId, String resetToken) {
        String subject = "Sound Marketplace - Password Reset Request";
        String body = String.format(
                "Hello,\n\nWe received a request to reset your password. Use the secure token below to reset your password:\n\n" +
                "Reset Token: %s\n\n" +
                "Alternatively, reset via: %s/api/auth/reset-password?token=%s\n\n" +
                "This token is valid for 30 minutes. If you did not request this, please disregard this email.\n\n" +
                "The Sound Marketplace Team", resetToken, appBaseUrl, resetToken);

        sendEmail(recipientEmail, userId, subject, body);
    }

    @Override
    public void sendUsernameRecoveryEmail(String recipientEmail, Integer userId, String username) {
        String subject = "Sound Marketplace - Username Recovery";
        String body = String.format(
                "Hello,\n\nYour Sound Marketplace username/display name is: %s\n" +
                "Registered email: %s\n\n" +
                "You can log in to your account anytime.\n\n" +
                "The Sound Marketplace Team", username, recipientEmail);

        sendEmail(recipientEmail, userId, subject, body);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void sendEmail(String recipientEmail, Integer userId, String subject, String body) {
        String status = "SENT";
        String errorMsg = null;

        try {
            log.info("Sending email via JavaMailSender to [***], subject: [{}]", subject);

            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(senderEmail);
            message.setTo(recipientEmail);
            message.setSubject(subject);
            message.setText(body);

            mailGateway.send(message);
            log.info("Successfully transmitted email to [***]");

        } catch (Exception e) {
            status = "FAILED";
            errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.error("Failed to physically transmit email via JavaMailSender to [***]: {}. Logging failure to email_logs.",
                    errorMsg, e);
        }

        // Record delivery audit log into MySQL database
        try {
            EmailLog emailLog = EmailLog.builder()
                    .userId(userId)
                    .recipientEmail(recipientEmail)
                    .senderEmail(senderEmail)
                    .subject(subject)
                    .body("[redacted]")
                    .status(status)
                    .errorMessage(errorMsg)
                    .sentAt(LocalDateTime.now())
                    .build();

            emailLogRepository.save(emailLog);
            log.info("Email log audit saved (status: {}, id: {})", status, emailLog.getId());
        } catch (Exception auditEx) {
            log.error("Could not write audit to email_logs table: {}", auditEx.getMessage());
        }
    }
}
