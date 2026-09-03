package com.mdl.platform.auth.controller;

import com.mdl.platform.auth.dto.LoginRequest;
import com.mdl.platform.auth.dto.LoginResponse;
import com.mdl.platform.auth.dto.MfaChallengeRequest;
import com.mdl.platform.auth.dto.MfaConfirmRequest;
import com.mdl.platform.auth.dto.MfaSetupResponse;
import com.mdl.platform.auth.dto.RefreshTokenRequest;
import com.mdl.platform.auth.dto.AuthUserResponse;
import com.mdl.platform.auth.service.AuthService;
import com.mdl.platform.auth.service.MfaService;
import com.mdl.platform.common.dto.ApiResponse;
import com.mdl.platform.security.AuthCookieSupport;
import com.mdl.platform.security.SecurityUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final MfaService mfaService;
    private final AuthCookieSupport authCookieSupport;

    public AuthController(AuthService authService, MfaService mfaService, AuthCookieSupport authCookieSupport) {
        this.authService = authService;
        this.mfaService = mfaService;
        this.authCookieSupport = authCookieSupport;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResponse response = authService.login(request, httpRequest);
        if (Boolean.TRUE.equals(response.mfaRequired())) {
            return ResponseEntity.ok(ApiResponse.ok("MFA required", response));
        }
        authCookieSupport.writeRefreshCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", withoutRefreshBody(response)));
    }

    @PostMapping("/mfa/challenge")
    public ResponseEntity<ApiResponse<LoginResponse>> mfaChallenge(
            @Valid @RequestBody MfaChallengeRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        LoginResponse response = authService.completeMfaChallenge(request, httpRequest);
        authCookieSupport.writeRefreshCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Login successful", withoutRefreshBody(response)));
    }

    @PostMapping("/mfa/setup")
    public ResponseEntity<ApiResponse<MfaSetupResponse>> mfaSetup() {
        var context = SecurityUtils.requireCurrentUser();
        MfaSetupResponse response = mfaService.beginSetup(context.userId(), context.email());
        return ResponseEntity.ok(ApiResponse.ok("MFA setup started", response));
    }

    @PostMapping("/mfa/confirm")
    public ResponseEntity<ApiResponse<Void>> mfaConfirm(@Valid @RequestBody MfaConfirmRequest request) {
        var context = SecurityUtils.requireCurrentUser();
        mfaService.confirmSetup(context.userId(), request.code());
        return ResponseEntity.ok(ApiResponse.ok("MFA enabled", null));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : authCookieSupport.readRefreshCookie(httpRequest).orElse(null);
        if (refreshToken == null) {
            return ResponseEntity.status(401).body(ApiResponse.error("Refresh token required"));
        }
        LoginResponse response = authService.refresh(refreshToken);
        authCookieSupport.writeRefreshCookie(httpResponse, response.refreshToken());
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", withoutRefreshBody(response)));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestBody(required = false) RefreshTokenRequest request,
            HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        String refreshToken = request != null && request.refreshToken() != null && !request.refreshToken().isBlank()
                ? request.refreshToken()
                : authCookieSupport.readRefreshCookie(httpRequest).orElse(null);
        if (refreshToken != null) {
            authService.logout(refreshToken);
        }
        authCookieSupport.clearRefreshCookie(httpResponse);
        return ResponseEntity.ok(ApiResponse.ok("Logged out", null));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<AuthUserResponse>> me() {
        return ResponseEntity.ok(ApiResponse.ok(authService.currentUser()));
    }

    private LoginResponse withoutRefreshBody(LoginResponse response) {
        return new LoginResponse(
                response.accessToken(),
                null,
                response.tokenType(),
                response.expiresInMinutes(),
                response.user(),
                response.mfaRequired(),
                response.mfaToken());
    }
}
