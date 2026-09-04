package com.mdl.platform.security;

import com.mdl.platform.users.repository.UserRepository;
import com.mdl.platform.users.repository.UserSessionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SessionValidationService {

    private final UserSessionRepository userSessionRepository;
    private final UserRepository userRepository;

    public SessionValidationService(
            UserSessionRepository userSessionRepository,
            UserRepository userRepository) {
        this.userSessionRepository = userSessionRepository;
        this.userRepository = userRepository;
    }

    public boolean isAccessAllowed(Long sessionId, Long userId) {
        if (sessionId == null || userId == null) {
            return false;
        }

        return userSessionRepository.findById(sessionId)
                .filter(session -> userId.equals(session.getUserId()))
                .filter(session -> "ACTIVE".equals(session.getStatus()))
                .filter(session -> session.getRevokedAt() == null)
                .filter(session -> session.getExpiresAt().isAfter(java.time.Instant.now()))
                .isPresent()
                && userRepository.findById(userId)
                        .map(user -> "ACTIVE".equals(user.getStatus()))
                        .orElse(false);
    }
}
