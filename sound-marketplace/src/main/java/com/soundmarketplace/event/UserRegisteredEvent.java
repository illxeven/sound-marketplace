package com.soundmarketplace.event;

public record UserRegisteredEvent(Integer userId, String email, String displayName, String rawVerificationToken) {}
