package com.example.dop.config;

import com.example.dop.util.RetryableRestClient;
import com.example.dop.util.apisix.ApisixClientRegistry;
import com.example.dop.util.apisix.ApisixTokenInterceptor;
import com.example.dop.util.apisix.ApisixTokenProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableConfigurationProperties(ApisixProperties.class)
public class ApisixClientRegistryConfig {

    @Bean
    public ApisixClientRegistry apisixClientRegistry(ApisixProperties props) {
        ApisixResilienceProperties r = props.resilience();
        Retry retry = buildRetry(r);
        CircuitBreaker circuitBreaker = buildCircuitBreaker(r);
        TimeLimiter timeLimiter = buildTimeLimiter(r);

        Map<String, RetryableRestClient> clients = new HashMap<>();
        props.gateways().forEach((name, gwProps) -> {
            ApisixTokenProvider tokenProvider = buildTokenProvider(gwProps);
            ApisixTokenInterceptor interceptor = new ApisixTokenInterceptor(
                    tokenProvider, gwProps.tokenHeaderName(), gwProps.tokenHeaderPrefix());
            RestClient apiRestClient = buildApiRestClient(gwProps, interceptor);
            clients.put(name, new RetryableRestClient(apiRestClient, retry, circuitBreaker, timeLimiter));
        });

        return new ApisixClientRegistry(clients);
    }

    private ApisixTokenProvider buildTokenProvider(ApisixGatewayProperties props) {
        RestClient tokenRestClient = RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
        return new ApisixTokenProvider(
                tokenRestClient,
                props.j1Token(),
                props.j1HeaderName(),
                props.tokenEndpoint(),
                props.tokenResponseField(),
                props.tokenTtlSeconds());
    }

    private RestClient buildApiRestClient(ApisixGatewayProperties props, ApisixTokenInterceptor interceptor) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptors(list -> list.add(interceptor))
                .build();
    }

    private Retry buildRetry(ApisixResilienceProperties r) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(r.maxAttempts())
                .waitDuration(Duration.ofMillis(1000))
                .retryOnException(t -> t instanceof HttpServerErrorException
                        || t instanceof ResourceAccessException
                        || t instanceof IOException)
                .ignoreExceptions(IllegalArgumentException.class, HttpClientErrorException.class)
                .build();
        return Retry.of("apisix-retry", config);
    }

    private CircuitBreaker buildCircuitBreaker(ApisixResilienceProperties r) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowSize(r.slidingWindowSize())
                .minimumNumberOfCalls(5)
                .failureRateThreshold(r.failureRateThreshold())
                .waitDurationInOpenState(Duration.ofMillis(r.waitDurationMs()))
                .permittedNumberOfCallsInHalfOpenState(3)
                .automaticTransitionFromOpenToHalfOpenEnabled(true)
                .recordExceptions(
                        HttpServerErrorException.class,
                        ResourceAccessException.class,
                        java.net.SocketTimeoutException.class,
                        IOException.class)
                .ignoreExceptions(IllegalArgumentException.class, HttpClientErrorException.class)
                .build();
        return CircuitBreaker.of("apisix-circuit-breaker", config);
    }

    private TimeLimiter buildTimeLimiter(ApisixResilienceProperties r) {
        return TimeLimiter.of("apisix-time-limiter", TimeLimiterConfig.custom()
                .timeoutDuration(Duration.ofSeconds(r.timeoutSeconds()))
                .build());
    }
}
