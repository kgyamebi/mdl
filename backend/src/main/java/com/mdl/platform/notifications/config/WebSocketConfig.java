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
    private final boolean allowLanOrigins;

    public WebSocketConfig(
            NotificationWebSocketHandler notificationWebSocketHandler,
            JwtWebSocketHandshakeInterceptor jwtWebSocketHandshakeInterceptor,
            @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}") String allowedOrigins,
            @Value("${app.cors.allow-lan-origins:true}") boolean allowLanOrigins) {
        this.notificationWebSocketHandler = notificationWebSocketHandler;
        this.jwtWebSocketHandshakeInterceptor = jwtWebSocketHandshakeInterceptor;
        this.allowedOrigins = allowedOrigins;
        this.allowLanOrigins = allowLanOrigins;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        var registration = registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .addInterceptors(jwtWebSocketHandshakeInterceptor);

        if (allowLanOrigins) {
            registration.setAllowedOriginPatterns(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "http://192.168.*:*",
                    "http://10.*:*",
                    "http://172.*:*"
            );
        } else {
            registration.setAllowedOrigins(allowedOrigins.split(","));
        }
    }
}
