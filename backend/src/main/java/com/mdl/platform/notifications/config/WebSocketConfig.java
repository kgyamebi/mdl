package com.mdl.platform.notifications.config;

import com.mdl.platform.notifications.realtime.JwtWebSocketHandshakeInterceptor;
import com.mdl.platform.notifications.realtime.NotificationWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final NotificationWebSocketHandler notificationWebSocketHandler;
    private final JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor;
    private final String allowedOrigins;

    public WebSocketConfig(
            NotificationWebSocketHandler notificationWebSocketHandler,
            JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor,
            @Value("${app.cors.allowed-origins:http://localhost:5173}") String allowedOrigins) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.jwtWebSocketHandshakeInterceptor = jwtWebSocketHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(jwtWebSocketHandshakeInterceptor)
                .setAllowedOriginPatterns(allowedOrigins.split(","));
    }
}
