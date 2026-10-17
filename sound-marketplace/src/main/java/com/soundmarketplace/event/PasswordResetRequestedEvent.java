package com.soundmarketplace.event;

public record PasswordResetRequestedEvent(Integer userId, String email, String rawResetToken) {}
