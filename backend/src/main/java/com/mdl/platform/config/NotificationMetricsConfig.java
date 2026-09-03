package com.mdl.platform.config;

import com.mdl.platform.notifications.realtime.NotificationRealtimeBroadcaster;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NotificationMetricsConfig {

    public NotificationMetricsConfig(
            MeterRegistry meterRegistry,
            NotificationRealtimeBroadcaster broadcaster) {
        Gauge.builder("mdl.notification.websocket.sessions", broadcaster::activeSessionCount)
                .description("Active WebSocket sessions for live notification delivery")
                .register(meterRegistry);
    }
}
