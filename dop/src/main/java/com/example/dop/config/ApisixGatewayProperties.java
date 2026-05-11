package com.example.dop.config;

public record ApisixGatewayProperties(
        String baseUrl,
        String j1Token,
        String tokenEndpoint,
        String j1HeaderName,
        String tokenResponseField,
        String tokenHeaderName,
        String tokenHeaderPrefix,
        long tokenTtlSeconds) {

    public ApisixGatewayProperties {
        j1HeaderName = defaultIfBlank(j1HeaderName, "X-J1-Token");
        tokenResponseField = defaultIfBlank(tokenResponseField, "token");
        tokenHeaderName = defaultIfBlank(tokenHeaderName, "Authorization");
        tokenHeaderPrefix = (tokenHeaderPrefix == null) ? "Bearer" : tokenHeaderPrefix;
        tokenTtlSeconds = tokenTtlSeconds <= 0 ? 300L : tokenTtlSeconds;
    }

    private static String defaultIfBlank(String value, String fallback) {
        return (value == null || value.isBlank()) ? fallback : value;
    }
}
