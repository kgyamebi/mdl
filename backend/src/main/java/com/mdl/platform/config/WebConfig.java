package com.mdl.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * CORS configuration — allows the React frontend to call the API during development.
 * Production origins are set via environment variable.
 */
@Configuration
public class WebConfig {

    @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173}")
    private String allowedOrigins;

    @Value("${app.cors.allow-lan-origins:true}")
    private boolean allowLanOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.setAllowedHeaders(List.of("*"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        if (allowLanOrigins) {
            config.setAllowedOriginPatterns(List.of(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "http://192.168.*:*",
                    "http://10.*:*",
                    "http://172.*:*"
            ));
        } else {
            config.setAllowedOrigins(Arrays.asList(allowedOrigins.split(",")));
        }

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws/**", config);
        return source;
    }

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                var mapping = registry.addMapping("/api/**")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                if (allowLanOrigins) {
                    mapping.allowedOriginPatterns(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "http://192.168.*:*",
                            "http://10.*:*",
                            "http://172.*:*"
                    );
                } else {
                    mapping.allowedOrigins(allowedOrigins.split(","));
                }

                var wsMapping = registry.addMapping("/ws/**")
                        .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                        .allowedHeaders("*")
                        .allowCredentials(true);

                if (allowLanOrigins) {
                    wsMapping.allowedOriginPatterns(
                            "http://localhost:*",
                            "http://127.0.0.1:*",
                            "http://192.168.*:*",
                            "http://10.*:*",
                            "http://172.*:*"
                    );
                } else {
                    wsMapping.allowedOrigins(allowedOrigins.split(","));
                }
            }
        };
    }
}
