package com.soundmarketplace.service;

import com.soundmarketplace.event.PasswordResetRequestedEvent;
import com.soundmarketplace.event.UserRegisteredEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
@Slf4j
public class EmailEventListener {

    private final EmailService emailService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onUserRegistered(UserRegisteredEvent event) {
        log.info("EVENT user.registered.email_trigger userId={}", event.userId());
        try {
            emailService.sendVerificationEmail(event.userId(), event.rawVerificationToken());
        } catch (Exception e) {
            log.error("Failed to send verification email for userId={}", event.userId(), e);
        }
    }

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        log.info("EVENT auth.password_reset.email_trigger userId={}", event.userId());
        try {
            emailService.sendPasswordResetEmail(event.email(), event.userId(), event.rawResetToken());
        } catch (Exception e) {
            log.error("Failed to send password reset email for userId={}", event.userId(), e);
        }
    }
}
