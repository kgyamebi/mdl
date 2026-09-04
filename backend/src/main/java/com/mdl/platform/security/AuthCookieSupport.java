package com.mdl.platform.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Optional;

@Component
public class AuthCookieSupport {

    public static final String REFRESH_COOKIE = "mdl_refresh_token";

    @Value("${app.auth.refresh-cookie-secure:false}")
    private boolean secureCookie;

    @Value("${app.jwt.refresh-token-expiry-days:7}")
    private long refreshTokenExpiryDays;

    public void writeRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, refreshToken)
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .sameSite(secureCookie ? "None" : "Lax")
                .maxAge(refreshTokenExpiryDays * 24 * 60 * 60)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE, "")
                .httpOnly(true)
                .secure(secureCookie)
                .path("/api/auth")
                .sameSite(secureCookie ? "None" : "Lax")
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    public Optional<String> readRefreshCookie(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> REFRESH_COOKIE.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }
}
