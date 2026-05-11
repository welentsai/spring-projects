package com.example.dop.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Map;

@ConfigurationProperties(prefix = "apisix")
public record ApisixProperties(
        Map<String, ApisixGatewayProperties> gateways,
        ApisixResilienceProperties resilience) {

    public ApisixProperties {
        if (gateways == null || gateways.isEmpty()) {
            throw new IllegalStateException(
                    "At least one APISIX gateway must be configured under 'apisix.gateways'");
        }
        if (resilience == null) {
            resilience = new ApisixResilienceProperties(3, 30L, 50.0f, 60_000L, 10);
        }
    }
}
