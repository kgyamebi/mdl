package com.mdl.platform.auth.service;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MfaChallengeStore {

    private static final long TTL_SECONDS = 300;

    private final Map<String, PendingChallenge> challenges = new ConcurrentHashMap<>();

    public String issue(Long userId) {
        String token = UUID.randomUUID().toString();
        challenges.put(token, new PendingChallenge(userId, Instant.now().plusSeconds(TTL_SECONDS)));
        return token;
    }

    public Optional<Long> consume(String token) {
        PendingChallenge challenge = challenges.remove(token);
        if (challenge == null || challenge.expiresAt().isBefore(Instant.now())) {
            return Optional.empty();
        }
        return Optional.of(challenge.userId());
    }

    private record PendingChallenge(Long userId, Instant expiresAt) {
    }
}
