package com.mdl.platform.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class JwtService implements TokenIssuer {

    private final SecretKey secretKey;
    private final long accessTokenExpiryMinutes;
    private final long refreshTokenExpiryDays;

    public JwtService(
            @Value("${app.jwt.secret}") String secret,
            @Value("${app.jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes,
            @Value("${app.jwt.refresh-token-expiry-days:7}") long refreshTokenExpiryDays) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
        this.refreshTokenExpiryDays = refreshTokenExpiryDays;
    }

    @Override
    public String generateAccessToken(
            Long userId,
            String email,
            String username,
            Long businessId,
            String businessCode,
            String currencyCode,
            Set<String> roles,
            Set<String> permissions,
            Long sessionId) {
        Instant now = Instant.now();
        Instant expiry = now.plusSeconds(accessTokenExpiryMinutes * 60);

        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(String.valueOf(userId))
                .claim("email", email)
                .claim("username", username)
                .claim("businessId", businessId)
                .claim("businessCode", businessCode)
                .claim("currencyCode", currencyCode)
                .claim("roles", roles)
                .claim("permissions", permissions)
                .claim("sessionId", sessionId)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))
                .signWith(secretKey)
                .compact();
    }

    @Override
    public String generateRefreshToken() {
        return UUID.randomUUID() + "." + UUID.randomUUID();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public UserContext toUserContext(Claims claims) {
        Long userId = Long.valueOf(claims.getSubject());
        String email = claims.get("email", String.class);
        String username = claims.get("username", String.class);
        Long businessId = claims.get("businessId", Number.class).longValue();
        String businessCode = claims.get("businessCode", String.class);
        String currencyCode = claims.get("currencyCode", String.class);
        Long sessionId = claims.get("sessionId", Number.class).longValue();

        @SuppressWarnings("unchecked")
        List<String> rolesList = claims.get("roles", List.class);
        @SuppressWarnings("unchecked")
        List<String> permissionsList = claims.get("permissions", List.class);

        return new UserContext(
                userId,
                email,
                username,
                businessId,
                businessCode,
                currencyCode,
                Set.copyOf(rolesList),
                Set.copyOf(permissionsList),
                sessionId
        );
    }

    @Override
    public long getRefreshTokenExpiryDays() {
        return refreshTokenExpiryDays;
    }
}
