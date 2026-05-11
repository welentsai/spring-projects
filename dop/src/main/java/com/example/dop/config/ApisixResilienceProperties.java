package com.example.dop.config;

public record ApisixResilienceProperties(
        int maxAttempts,
        long timeoutSeconds,
        float failureRateThreshold,
        long waitDurationMs,
        int slidingWindowSize) {

    public ApisixResilienceProperties {
        maxAttempts = maxAttempts <= 0 ? 3 : maxAttempts;
        timeoutSeconds = timeoutSeconds <= 0 ? 30L : timeoutSeconds;
        failureRateThreshold = failureRateThreshold <= 0 ? 50.0f : failureRateThreshold;
        waitDurationMs = waitDurationMs <= 0 ? 60_000L : waitDurationMs;
        slidingWindowSize = slidingWindowSize <= 0 ? 10 : slidingWindowSize;
    }
}
