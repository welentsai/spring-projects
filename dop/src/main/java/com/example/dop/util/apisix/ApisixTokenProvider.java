package com.example.dop.util.apisix;

import com.example.dop.util.Success;
import com.example.dop.util.Try;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ApisixTokenProvider {

    private record TokenState(String token, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }

    private final RestClient tokenRestClient;
    private final String j1Token;
    private final String j1HeaderName;
    private final String tokenEndpoint;
    private final String tokenResponseField;
    private final long tokenTtlSeconds;

    private volatile TokenState cachedToken = null;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ApisixTokenProvider(
            RestClient tokenRestClient,
            String j1Token,
            String j1HeaderName,
            String tokenEndpoint,
            String tokenResponseField,
            long tokenTtlSeconds) {
        this.tokenRestClient = tokenRestClient;
        this.j1Token = j1Token;
        this.j1HeaderName = j1HeaderName;
        this.tokenEndpoint = tokenEndpoint;
        this.tokenResponseField = tokenResponseField;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public String getToken() {
        // Fast path: shared read lock — concurrent callers don't block each other
        lock.readLock().lock();
        try {
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.token();
            }
        } finally {
            lock.readLock().unlock();
        }

        // Slow path: exclusive write lock with double-check to prevent thundering herd
        lock.writeLock().lock();
        try {
            if (cachedToken != null && !cachedToken.isExpired()) {
                return cachedToken.token();
            }

            Try<String> result = fetchToken();
            if (result instanceof Success<String> s) {
                cachedToken = new TokenState(
                        s.getResult(),
                        Instant.now().plusSeconds(tokenTtlSeconds));
                return cachedToken.token();
            }
            throw new ApisixTokenException(
                    "Failed to fetch J2 token: " + result.getError().getMessage(),
                    result.getError());
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void invalidate() {
        lock.writeLock().lock();
        try {
            cachedToken = null;
        } finally {
            lock.writeLock().unlock();
        }
    }

    private Try<String> fetchToken() {
        return Try.of(() -> {
            Map<String, String> response = tokenRestClient
                    .get()
                    .uri(tokenEndpoint)
                    .header(j1HeaderName, j1Token)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});

            if (response == null || !response.containsKey(tokenResponseField)) {
                throw new ApisixTokenException(
                        "Token field '" + tokenResponseField + "' not found in response");
            }
            return response.get(tokenResponseField);
        });
    }
}
