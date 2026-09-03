package com.mdl.platform.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AppInfoContributor implements InfoContributor {

    @Value("${app.name:MDL Platform}")
    private String appName;

    @Value("${app.version:0.1.0}")
    private String appVersion;

    @Override
    public void contribute(Info.Builder builder) {
        builder.withDetail("application", Map.of(
                "name", appName,
                "version", appVersion));
    }
}
