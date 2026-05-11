package com.example.dop.util.apisix;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

class ApisixTokenProviderTest {

    private static final int TOKEN_SERVER_PORT = 8091;
    private static final String TOKEN_ENDPOINT = "/apisix/token";
    private static final String J1_TOKEN = "test-j1-token";
    private static final String J1_HEADER = "X-J1-Token";
    private static final String TOKEN_FIELD = "token";

    private WireMockServer tokenServer;
    private ApisixTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenServer = new WireMockServer(wireMockConfig().port(TOKEN_SERVER_PORT));
        tokenServer.start();

        RestClient tokenRestClient = RestClient.builder()
                .baseUrl("http://localhost:" + TOKEN_SERVER_PORT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        tokenProvider = new ApisixTokenProvider(
                tokenRestClient, J1_TOKEN, J1_HEADER, TOKEN_ENDPOINT, TOKEN_FIELD, 300L);
    }

    @AfterEach
    void tearDown() {
        tokenServer.stop();
    }

    @Test
    void getToken_success() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .withHeader(J1_HEADER, equalTo(J1_TOKEN))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-abc\"}")));

        String token = tokenProvider.getToken();

        Assertions.assertEquals("j2-abc", token);
        tokenServer.verify(1, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }

    @Test
    void getToken_cachedOnSecondCall() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-cached\"}")));

        String first = tokenProvider.getToken();
        String second = tokenProvider.getToken();

        Assertions.assertEquals("j2-cached", first);
        Assertions.assertEquals("j2-cached", second);
        // Cache hit: token endpoint called only once
        tokenServer.verify(1, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }

    @Test
    void getToken_refreshesAfterTtlExpiry() throws InterruptedException {
        tokenProvider = new ApisixTokenProvider(
                RestClient.builder()
                        .baseUrl("http://localhost:" + TOKEN_SERVER_PORT)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build(),
                J1_TOKEN, J1_HEADER, TOKEN_ENDPOINT, TOKEN_FIELD,
                1L); // 1-second TTL

        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Token Refresh")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-first\"}"))
                .willSetStateTo("Refreshed"));

        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Token Refresh")
                .whenScenarioStateIs("Refreshed")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-second\"}")));

        String first = tokenProvider.getToken();
        Thread.sleep(1500); // wait for TTL expiry
        String second = tokenProvider.getToken();

        Assertions.assertEquals("j2-first", first);
        Assertions.assertEquals("j2-second", second);
        tokenServer.verify(2, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }

    @Test
    void getToken_refreshesAfterInvalidate() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Invalidate")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-v1\"}"))
                .willSetStateTo("Invalidated"));

        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Invalidate")
                .whenScenarioStateIs("Invalidated")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-v2\"}")));

        String first = tokenProvider.getToken();
        tokenProvider.invalidate();
        String second = tokenProvider.getToken();

        Assertions.assertEquals("j2-v1", first);
        Assertions.assertEquals("j2-v2", second);
        tokenServer.verify(2, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }

    @Test
    void getToken_concurrentAccess_onlyOneFetch() throws InterruptedException {
        // 50ms delay forces threads to be truly concurrent at the write lock
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withFixedDelay(50)
                        .withBody("{\"token\":\"j2-concurrent\"}")));

        int threadCount = 10;
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        List<String> results = Collections.synchronizedList(new ArrayList<>());
        List<Throwable> errors = Collections.synchronizedList(new ArrayList<>());

        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startGate.await();
                    results.add(tokenProvider.getToken());
                } catch (Throwable t) {
                    errors.add(t);
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }

        startGate.countDown(); // release all threads simultaneously
        doneLatch.await(10, TimeUnit.SECONDS);

        Assertions.assertTrue(errors.isEmpty(), "No thread should have thrown: " + errors);
        Assertions.assertEquals(threadCount, results.size());
        results.forEach(t -> Assertions.assertEquals("j2-concurrent", t));
        // Double-checked locking: only 1 HTTP call despite 10 concurrent threads
        tokenServer.verify(1, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
    }

    @Test
    void getToken_throwsApisixTokenException_onServerError() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        ApisixTokenException ex = Assertions.assertThrows(
                ApisixTokenException.class,
                () -> tokenProvider.getToken());

        Assertions.assertTrue(ex.getMessage().contains("Failed to fetch J2 token"));
    }

    @Test
    void getToken_throwsApisixTokenException_whenFieldMissing() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"access_token\":\"j2-wrong-field\"}")));

        ApisixTokenException ex = Assertions.assertThrows(
                ApisixTokenException.class,
                () -> tokenProvider.getToken());

        Assertions.assertTrue(ex.getMessage().contains("'token' not found in response"),
                "Expected message to mention missing field, got: " + ex.getMessage());
    }
}
