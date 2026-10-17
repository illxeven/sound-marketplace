package com.soundmarketplace.gateway;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MailGateway {

    private final JavaMailSender mailSender;

    public void send(SimpleMailMessage message) {
        long start = System.currentTimeMillis();
        String outcome = "ok";
        String code = "200";
        try {
            mailSender.send(message);
        } catch (Exception e) {
            outcome = "error";
            code = e.getClass().getSimpleName();
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - start;
            log.info("EXT_CALL system=mail op=send durationMs={} outcome={} code={}", duration, outcome, code);
        }
    }
}
