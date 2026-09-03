package com.mdl.platform.notifications.realtime;

import com.mdl.platform.security.JwtService;
import com.mdl.platform.security.UserContext;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class JwtWebSocketHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTR = "userId";
    public static final String BUSINESS_ID_ATTR = "businessId";

    private final JwtService jwtService;

    public JwtWebSocketHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Map<String, Object> attributes) {
        if (!(request instanceof ServletServerHttpRequest servletRequest)) {
            return false;
        }

        String token = servletRequest.getServletRequest().getParameter("token");
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            UserContext context = jwtService.toUserContext(jwtService.parseClaims(token));
            attributes.put(USER_ID_ATTR, context.userId());
            attributes.put(BUSINESS_ID_ATTR, context.businessId());
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public void afterHandshake(
            ServerHttpRequest request,
            ServerHttpResponse response,
            WebSocketHandler wsHandler,
            Exception exception) {
        // no-op
    }
}
