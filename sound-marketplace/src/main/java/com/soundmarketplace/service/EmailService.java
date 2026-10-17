package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.UserAccount;

public interface EmailService {
    void sendWelcomeEmail(UserAccount user, String displayName);
    void sendVerificationEmail(UserAccount user, String displayName, String token);
    void sendPasswordResetEmail(String recipientEmail, Integer userId, String resetToken);
    void sendUsernameRecoveryEmail(String recipientEmail, Integer userId, String username);
    void sendEmail(String recipientEmail, Integer userId, String subject, String body);
}
