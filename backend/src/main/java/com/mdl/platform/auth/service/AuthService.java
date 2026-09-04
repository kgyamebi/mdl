package com.mdl.platform.auth.service;

import com.mdl.platform.alerts.service.AlertNotifier;
import com.mdl.platform.audit.service.AuditRecorder;
import com.mdl.platform.auth.dto.AuthUserResponse;
import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.auth.dto.LoginResponse;
import com.mdl.platform.auth.dto.MfaChallengeRequest;
import com.mdl.platform.authorization.projection.UserAuthProfile;
import com.mdl.platform.authorization.repository.UserAuthProfileRepository;
import com.mdl.platform.common.exception.UnauthorizedException;
import com.mdl.platform.security.TokenIssuer;
import com.mdl.platform.security.SecurityUtils;
import com.mdl.platform.security.TokenHasher;
import com.mdl.platform.security.UserContext;
import com.mdl.platform.users.entity.User;
import com.mdl.platform.users.entity.UserSession;
import com.mdl.platform.users.repository.UserRepository;
import com.mdl.platform.users.repository.UserSessionRepository;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class AuthService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final UserAuthProfileRepository userAuthProfileRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final AuditRecorder auditRecorder;
    private final AlertNotifier alertNotifier;
    private final MfaChallengeStore mfaChallengeStore;
    private final MfaService mfaService;
    private final long accessTokenExpiryMinutes;

    public AuthService(
            UserRepository userRepository,
            UserSessionRepository userSessionRepository,
            UserAuthProfileRepository userAuthProfileRepository,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            AuditRecorder auditRecorder,
            AlertNotifier alertNotifier,
            MfaChallengeStore mfaChallengeStore,
            MfaService mfaService,
            @Value("${app.jwt.access-token-expiry-minutes:15}") long accessTokenExpiryMinutes) {
        this.userRepository = userRepository;
        this.userSessionRepository = userSessionRepository;
        this.userAuthProfileRepository = userAuthProfileRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.auditRecorder = auditRecorder;
        this.alertNotifier = alertNotifier;
        this.mfaChallengeStore = mfaChallengeStore;
        this.mfaService = mfaService;
        this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
    }

    @Transactional
    public LoginResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository
                .findByEmailIgnoreCaseOrUsernameIgnoreCase(request.login(), request.login())
                .orElseThrow(() -> new UnauthorizedException("Invalid email/username or password"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }

        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now())) {
            throw new UnauthorizedException("Account is temporarily locked. Try again later.");
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            registerFailedLogin(user, httpRequest);
            throw new UnauthorizedException("Invalid email/username or password");
        }

        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        userRepository.save(user);

        if (user.isMfaEnabled()) {
            String mfaToken = mfaChallengeStore.issue(user.getId());
            return LoginResponse.mfaChallenge(mfaToken);
        }

        return issueLoginResponse(user, httpRequest);
    }

    @Transactional
    public LoginResponse completeMfaChallenge(MfaChallengeRequest request, HttpServletRequest httpRequest) {
        Long userId = mfaChallengeStore.consume(request.mfaToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired MFA challenge"));

        if (!mfaService.verifyUserCode(userId, request.code())) {
            throw new UnauthorizedException("Invalid verification code");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        return issueLoginResponse(user, httpRequest);
    }

    private LoginResponse issueLoginResponse(User user, HttpServletRequest httpRequest) {
        UserAuthProfile.BusinessProfile business = userAuthProfileRepository
                .findDefaultBusiness(user.getId())
                .orElseThrow(() -> new UnauthorizedException("No active business membership found"));

        Set<String> roles = new HashSet<>(userAuthProfileRepository.findRoleCodes(
                user.getId(), business.getBusinessId()));
        Set<String> permissions = new HashSet<>(userAuthProfileRepository.findPermissionCodes(
                user.getId(), business.getBusinessId()));

        String refreshToken = tokenIssuer.generateRefreshToken();
        UserSession session = createSession(user, business.getBusinessId(), refreshToken, httpRequest);
        userSessionRepository.save(session);

        String accessToken = tokenIssuer.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                business.getBusinessId(),
                business.getBusinessCode(),
                business.getCurrencyCode(),
                roles,
                permissions,
                session.getId());

        AuthUserResponse userResponse = toAuthUserResponse(user, business, roles, permissions);

        auditRecorder.record(
                business.getBusinessId(),
                user.getId(),
                "LOGIN_SUCCESS",
                "AUTH",
                "USER",
                user.getId(),
                user.getEmail(),
                "User logged in",
                Map.of("businessCode", business.getBusinessCode()),
                httpRequest);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiryMinutes,
                userResponse);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        String refreshHash = TokenHasher.hash(refreshToken);
        UserSession session = userSessionRepository
                .findByRefreshTokenHashAndStatus(refreshHash, "ACTIVE")
                .filter(UserSession::isActive)
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        User user = userRepository.findById(session.getUserId())
                .orElseThrow(() -> new UnauthorizedException("Invalid or expired refresh token"));

        if (!"ACTIVE".equals(user.getStatus())) {
            throw new UnauthorizedException("Account is not active");
        }

        UserAuthProfile.BusinessProfile business = userAuthProfileRepository
                .findDefaultBusiness(user.getId())
                .orElseThrow(() -> new UnauthorizedException("No active business membership found"));

        Set<String> roles = new HashSet<>(userAuthProfileRepository.findRoleCodes(
                user.getId(), business.getBusinessId()));
        Set<String> permissions = new HashSet<>(userAuthProfileRepository.findPermissionCodes(
                user.getId(), business.getBusinessId()));

        session.setLastActivityAt(Instant.now());
        userSessionRepository.save(session);

        String accessToken = tokenIssuer.generateAccessToken(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                business.getBusinessId(),
                business.getBusinessCode(),
                business.getCurrencyCode(),
                roles,
                permissions,
                session.getId());

        AuthUserResponse userResponse = toAuthUserResponse(user, business, roles, permissions);

        return new LoginResponse(
                accessToken,
                refreshToken,
                "Bearer",
                accessTokenExpiryMinutes,
                userResponse);
    }

    @Transactional
    public void logout(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return;
        }

        String refreshHash = TokenHasher.hash(refreshToken);
        userSessionRepository.findByRefreshTokenHashAndStatus(refreshHash, "ACTIVE")
                .ifPresent(session -> {
                    session.setStatus("REVOKED");
                    session.setRevokedAt(Instant.now());
                    userSessionRepository.save(session);

                    auditRecorder.record(
                            session.getBusinessId(),
                            session.getUserId(),
                            "LOGOUT",
                            "AUTH",
                            "USER",
                            session.getUserId(),
                            null,
                            "User logged out",
                            null,
                            null);
                });
    }

    public AuthUserResponse currentUser() {
        UserContext context = SecurityUtils.requireCurrentUser();
        User user = userRepository.findById(context.userId())
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        UserAuthProfile.BusinessProfile business = userAuthProfileRepository
                .findDefaultBusiness(user.getId())
                .orElseThrow(() -> new UnauthorizedException("No active business membership found"));

        Set<String> roles = new HashSet<>(userAuthProfileRepository.findRoleCodes(
                user.getId(), business.getBusinessId()));
        Set<String> permissions = new HashSet<>(userAuthProfileRepository.findPermissionCodes(
                user.getId(), business.getBusinessId()));

        return toAuthUserResponse(user, business, roles, permissions);
    }

    private void registerFailedLogin(User user, HttpServletRequest httpRequest) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        Instant lockedUntil = null;

        if (attempts >= MAX_FAILED_ATTEMPTS) {
            lockedUntil = Instant.now().plus(LOCK_MINUTES, ChronoUnit.MINUTES);
            user.setLockedUntil(lockedUntil);
            user.setFailedLoginAttempts(0);
        }

        userRepository.save(user);

        Optional<UserAuthProfile.BusinessProfile> business = userAuthProfileRepository.findDefaultBusiness(user.getId());
        Instant accountLockedUntil = lockedUntil;
        business.ifPresent(profile -> {
            auditRecorder.record(
                    profile.getBusinessId(),
                    user.getId(),
                    "LOGIN_FAILED",
                    "AUTH",
                    "USER",
                    user.getId(),
                    user.getEmail(),
                    "Failed login attempt",
                    Map.of("failedAttempts", attempts),
                    httpRequest);

            alertNotifier.checkFailedLoginPattern(profile.getBusinessId(), user.getId(), user.getEmail());

            if (accountLockedUntil != null) {
                alertNotifier.notifyAccountLocked(
                        profile.getBusinessId(), user.getId(), user.getEmail(), accountLockedUntil);
            }
        });
    }

    private UserSession createSession(
            User user,
            Long businessId,
            String refreshToken,
            HttpServletRequest httpRequest) {
        UserSession session = new UserSession();
        session.setUserId(user.getId());
        session.setBusinessId(businessId);
        session.setRefreshTokenHash(TokenHasher.hash(refreshToken));
        session.setSessionTokenHash(TokenHasher.hash(refreshToken + user.getId()));
        session.setIpAddress(httpRequest.getRemoteAddr());
        session.setUserAgent(httpRequest.getHeader("User-Agent"));
        session.setExpiresAt(Instant.now().plus(tokenIssuer.getRefreshTokenExpiryDays(), ChronoUnit.DAYS));
        session.setLastActivityAt(Instant.now());
        return session;
    }

    private AuthUserResponse toAuthUserResponse(
            User user,
            UserAuthProfile.BusinessProfile business,
            Set<String> roles,
            Set<String> permissions) {
        return new AuthUserResponse(
                user.getId(),
                user.getEmail(),
                user.getUsername(),
                user.getFullName(),
                business.getBusinessId(),
                business.getBusinessCode(),
                business.getBusinessName(),
                business.getCurrencyCode(),
                user.isMfaEnabled(),
                roles,
                permissions);
    }
}
