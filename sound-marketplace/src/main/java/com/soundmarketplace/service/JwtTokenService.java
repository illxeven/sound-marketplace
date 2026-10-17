package com.soundmarketplace.service;

import com.soundmarketplace.sound_marketplace.domain.Role;
import com.soundmarketplace.sound_marketplace.domain.UserAccount;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtEncoder jwtEncoder;

    public static class TokenPair {
        private final String token;
        private final Instant expiresAt;

        public TokenPair(String token, Instant expiresAt) {
            this.token = token;
            this.expiresAt = expiresAt;
        }

        public String getToken() {
            return token;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    public TokenPair generateAccessToken(UserAccount user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(60, ChronoUnit.MINUTES);

        List<String> roles = user.getRoles().stream()
                .map(Role::getRoleName)
                .map(r -> r.toUpperCase(Locale.ROOT))
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sound-marketplace")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(user.getId()))
                .claim("roles", roles)
                .build();

        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenPair(token, expiresAt);
    }

    public String generateToken(Integer userId, List<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(60, ChronoUnit.MINUTES);
        List<String> upperRoles = roles != null
                ? roles.stream().map(r -> r.toUpperCase(Locale.ROOT)).toList()
                : List.of();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("sound-marketplace")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(String.valueOf(userId))
                .claim("roles", upperRoles)
                .build();
        JwsHeader header = JwsHeader.with(org.springframework.security.oauth2.jose.jws.MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
