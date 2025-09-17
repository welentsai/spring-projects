package com.example.dop.util;

import com.example.dop.util.exception.RetryableRestClientException;
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
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

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

    @Test
    public void test_get_with_retry_success() throws JsonProcessingException {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // Set up scenario: fail twice, succeed on third attempt
        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .inScenario("Retry Success")
                        .whenScenarioStateIs(STARTED)
                        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error"))
                        .willSetStateTo("First Retry"));

        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .inScenario("Retry Success")
                        .whenScenarioStateIs("First Retry")
                        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error"))
                        .willSetStateTo("Second Retry"));

        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .inScenario("Retry Success")
                        .whenScenarioStateIs("Second Retry")
                        .willReturn(
                                aResponse()
                                        .withHeader("Content-Type", "text/plain")
                                        .withStatus(200)
                                        .withBodyFile("get_react_success.json")));

        String resp = retryableRestClient.get(uri, String.class, params);

        ReactHackerNews hackerNews = JsonUtils.fromJson(resp, ReactHackerNews.class);
        Assertions.assertEquals(20, hackerNews.hitsPerPage());
        Assertions.assertEquals("advancedSyntax=true&analyticsTags=backend", hackerNews.params());

        // Verify 3 requests were made (initial + 2 retries)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo(uri)));
    }

    @Test
    public void test_get_with_retry_failure() {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // All attempts return 500 error
        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        RetryableRestClientException exception =
                Assertions.assertThrows(
                        RetryableRestClientException.class,
                        () -> retryableRestClient.get(uri, String.class, params));

        Assertions.assertTrue(
                exception.getMessage().contains("failed after applying all resilience patterns"));

        // Verify 3 requests were made (initial + 2 retries)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo(uri)));
    }

    @Test
    public void test_get_with_circuit_breaker_open() {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // All requests return 500 to trigger circuit breaker
        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        // Make 6 failing calls to open circuit breaker (minimum calls = 5, failure rate = 50%)
        for (int i = 0; i < 6; i++) {
            try {
                retryableRestClient.get(uri, String.class, params);
            } catch (Exception e) {
                // Expected failures
            }
        }

        // Next call should fail immediately due to open circuit breaker
        RetryableRestClientException exception =
                Assertions.assertThrows(
                        RetryableRestClientException.class,
                        () -> retryableRestClient.get(uri, String.class, params));

        Assertions.assertTrue(
                exception.getMessage().contains("failed after applying all resilience patterns"));
    }

    @Test
    public void test_get_with_timeout() {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // Response takes longer than 30 second timeout
        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .willReturn(
                                aResponse()
                                        .withStatus(200)
                                        .withFixedDelay(35000) // 35 seconds > 30 second timeout
                                        .withBodyFile("get_react_success.json")));

        RetryableRestClientException exception =
                Assertions.assertThrows(
                        RetryableRestClientException.class,
                        () -> retryableRestClient.get(uri, String.class, params));

        Assertions.assertTrue(
                exception.getMessage().contains("failed after applying all resilience patterns"));
    }

    @Test
    public void test_get_with_4xx_error() {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // Return 404 Not Found (4xx client error)
        wireMockServer.stubFor(
                get(urlEqualTo(uri)).willReturn(aResponse().withStatus(404).withBody("Not Found")));

        // 4xx errors should not be retried and should throw HttpClientErrorException directly
        Assertions.assertThrows(
                RetryableRestClientException.class,
                () -> retryableRestClient.get(uri, String.class, params));

        // Verify only 1 request was made (no retries for 4xx errors)
        wireMockServer.verify(1, getRequestedFor(urlEqualTo(uri)));
    }

    @Test
    public void test_get_with_5xx_error() {
        String uri = "/api/v1/search";
        QueryParams params = QueryParams.builder().add("query", "react").build();

        // Return 503 Service Unavailable (5xx server error)
        wireMockServer.stubFor(
                get(urlEqualTo(uri))
                        .willReturn(aResponse().withStatus(503).withBody("Service Unavailable")));

        // 5xx errors should be retried and eventually wrapped in RetryableRestClientException
        RetryableRestClientException exception =
                Assertions.assertThrows(
                        RetryableRestClientException.class,
                        () -> retryableRestClient.get(uri, String.class, params));

        Assertions.assertTrue(
                exception.getMessage().contains("failed after applying all resilience patterns"));

        // Verify 3 requests were made (initial + 2 retries)
        wireMockServer.verify(3, getRequestedFor(urlEqualTo(uri)));
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
