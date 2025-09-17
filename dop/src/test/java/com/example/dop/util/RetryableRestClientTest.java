package com.example.dop.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.tomakehurst.wiremock.WireMockServer;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.timelimiter.TimeLimiter;
import io.github.resilience4j.timelimiter.TimeLimiterConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;

public class RetryableRestClientTest {

    private WireMockServer wireMockServer;
    private RetryableRestClient retryableRestClient;

    @BeforeEach
    public void setUp() {
        System.out.println("Setup wiremock server !");
        wireMockServer =
                new WireMockServer(
                        wireMockConfig()
                                .port(8090)
                                .usingFilesUnderClasspath("src/test/resources/wiremock"));
        wireMockServer.start();

        // --- set up for retryable rest client --
        //        String baseUrl = "https://hn.algolia.com";
        String baseUrl = "http://localhost:" + wireMockServer.port();
        RestClient restClient = getRestClient(baseUrl);
        Retry retry = getRetry();
        CircuitBreaker circuitBreaker = getCircuitBreaker();
        TimeLimiter timeLimiter = getTimeLimiter();

        retryableRestClient =
                new RetryableRestClient(restClient, retry, circuitBreaker, timeLimiter);
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    @Test
    public void test_get_success() throws JsonProcessingException {
        String uri = "/api/v1/search";
        // Simple query parameters
        QueryParams params1 = QueryParams.builder().add("query", "react").build();

        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "text/plain")
                                        .withStatus(200)
                                        .withBodyFile("get_react_success.json")));

        String resp = retryableRestClient.get(uri, String.class, params1);

        //        System.out.println(resp);

        ReactHackerNews hackerNews = JsonUtils.fromJson(resp, ReactHackerNews.class);
        System.out.println(hackerNews);
        Assertions.assertEquals(20, hackerNews.hitsPerPage());
        Assertions.assertEquals("advancedSyntax=true&analyticsTags=backend", hackerNews.params());
    }

    private TimeLimiter getTimeLimiter() {
        TimeLimiterConfig config =
                TimeLimiterConfig.custom().timeoutDuration(Duration.ofMillis(30000)).build();

        return TimeLimiter.of("api-time-limiter", config);
    }

    private RestClient getRestClient(String baseUrl) {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "RetryableRestClient/1.0")
                .build();
    }

    private CircuitBreaker getCircuitBreaker() {
        CircuitBreakerConfig config =
                CircuitBreakerConfig.custom()
                        .slidingWindowSize(10)
                        .minimumNumberOfCalls(5)
                        .failureRateThreshold(50.0f)
                        .waitDurationInOpenState(Duration.ofMillis(60000))
                        .permittedNumberOfCallsInHalfOpenState(3)
                        .automaticTransitionFromOpenToHalfOpenEnabled(true)
                        .recordExceptions(
                                HttpServerErrorException.class,
                                ResourceAccessException.class,
                                java.net.SocketTimeoutException.class,
                                java.io.IOException.class)
                        .ignoreExceptions(
                                IllegalArgumentException.class, HttpClientErrorException.class)
                        .build();

        return CircuitBreaker.of("api-circuit-breaker", config);
    }

    private Retry getRetry() {
        RetryConfig config =
                RetryConfig.custom()
                        .maxAttempts(3)
                        .waitDuration(Duration.ofMillis(1000))
                        .retryOnException(
                                throwable ->
                                        throwable instanceof HttpServerErrorException
                                                || throwable instanceof ResourceAccessException
                                                || throwable instanceof java.io.IOException)
                        .ignoreExceptions(
                                IllegalArgumentException.class,
                                org.springframework.web.client.HttpClientErrorException.class)
                        .build();

        return Retry.of("api-retry", config);
    }
}
