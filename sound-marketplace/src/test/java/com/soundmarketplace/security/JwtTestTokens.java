package com.soundmarketplace.security;

import com.soundmarketplace.service.JwtTokenService;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class JwtTestTokens {

    private final JwtTokenService jwtTokenService;

    public JwtTestTokens(JwtTokenService jwtTokenService) {
        this.jwtTokenService = jwtTokenService;
    }

    public String token(Integer userId, String... roles) {
        return jwtTokenService.generateToken(userId, List.of(roles));
    }

    public String bearer(Integer userId, String... roles) {
        return "Bearer " + token(userId, roles);
    }

    public String consumer(Integer userId) {
        return bearer(userId, "CONSUMER");
    }

    public String artist(Integer userId) {
        return bearer(userId, "MUSIC_ARTIST");
    }

    public String admin(Integer userId) {
        return bearer(userId, "ADMIN");
    }
}
