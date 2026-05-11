package com.example.dop.util.apisix;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static com.github.tomakehurst.wiremock.stubbing.Scenario.STARTED;

class ApisixTokenInterceptorTest {

    private static final int TOKEN_SERVER_PORT = 8092;
    private static final int API_SERVER_PORT = 8093;
    private static final String TOKEN_ENDPOINT = "/apisix/token";
    private static final String API_ENDPOINT = "/api/resource";

    private WireMockServer tokenServer;
    private WireMockServer apiServer;
    private RestClient apiRestClient;
    private ApisixTokenProvider tokenProvider;

    @BeforeEach
    void setUp() {
        tokenServer = new WireMockServer(wireMockConfig().port(TOKEN_SERVER_PORT));
        tokenServer.start();

        apiServer = new WireMockServer(wireMockConfig().port(API_SERVER_PORT));
        apiServer.start();

        RestClient tokenRestClient = RestClient.builder()
                .baseUrl("http://localhost:" + TOKEN_SERVER_PORT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        tokenProvider = new ApisixTokenProvider(
                tokenRestClient, "j1-token", "X-J1-Token", TOKEN_ENDPOINT, "token", 300L);

        ApisixTokenInterceptor interceptor =
                new ApisixTokenInterceptor(tokenProvider, "Authorization", "Bearer");

        apiRestClient = RestClient.builder()
                .baseUrl("http://localhost:" + API_SERVER_PORT)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .requestInterceptors(interceptors -> interceptors.add(interceptor))
                .build();
    }

    @AfterEach
    void tearDown() {
        tokenServer.stop();
        apiServer.stop();
    }

    @Test
    void intercept_injectsJ2TokenHeader() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-test\"}")));

        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"data\":\"ok\"}")));

        apiRestClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

        // Verify the API request carried the correct Authorization header
        apiServer.verify(getRequestedFor(urlEqualTo(API_ENDPOINT))
                .withHeader("Authorization", equalTo("Bearer j2-test")));
    }

    @Test
    void intercept_on401_invalidatesAndRetries() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Token Refresh")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-stale\"}"))
                .willSetStateTo("Refreshed"));

        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .inScenario("Token Refresh")
                .whenScenarioStateIs("Refreshed")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-fresh\"}")));

        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .inScenario("API Retry")
                .whenScenarioStateIs(STARTED)
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized"))
                .willSetStateTo("Retry"));

        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .inScenario("API Retry")
                .whenScenarioStateIs("Retry")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"data\":\"ok\"}")));

        String result = apiRestClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

        Assertions.assertNotNull(result);
        // Initial 401 + 1 retry = 2 calls to API
        apiServer.verify(2, getRequestedFor(urlEqualTo(API_ENDPOINT)));
        // Original token fetch + refresh after invalidate = 2 calls to token endpoint
        tokenServer.verify(2, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
        // Retry used the fresh token
        apiServer.verify(1, getRequestedFor(urlEqualTo(API_ENDPOINT))
                .withHeader("Authorization", equalTo("Bearer j2-fresh")));
    }

    @Test
    void intercept_on401_doesNotRetryInfinitely() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-always\"}")));

        // API always returns 401
        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .willReturn(aResponse().withStatus(401).withBody("Unauthorized")));

        // Second 401 (after the retry) surfaces as HttpClientErrorException.Unauthorized
        Assertions.assertThrows(
                org.springframework.web.client.HttpClientErrorException.Unauthorized.class,
                () -> apiRestClient.get().uri(API_ENDPOINT).retrieve().body(String.class));

        // Exactly 2 API calls: original + 1 retry — no infinite loop
        apiServer.verify(2, getRequestedFor(urlEqualTo(API_ENDPOINT)));
    }

    @Test
    void intercept_customHeaderNameAndNoPrefix() {
        ApisixTokenProvider customProvider = new ApisixTokenProvider(
                RestClient.builder()
                        .baseUrl("http://localhost:" + TOKEN_SERVER_PORT)
                        .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                        .build(),
                "j1-token", "X-J1-Token", TOKEN_ENDPOINT, "token", 300L);

        ApisixTokenInterceptor customInterceptor =
                new ApisixTokenInterceptor(customProvider, "X-Api-Token", "");

        RestClient customClient = RestClient.builder()
                .baseUrl("http://localhost:" + API_SERVER_PORT)
                .requestInterceptors(list -> list.add(customInterceptor))
                .build();

        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"raw-j2\"}")));

        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"data\":\"ok\"}")));

        customClient.get().uri(API_ENDPOINT).retrieve().body(String.class);

        // No "Bearer " prefix — token value injected raw
        apiServer.verify(getRequestedFor(urlEqualTo(API_ENDPOINT))
                .withHeader("X-Api-Token", equalTo("raw-j2")));
    }

    @Test
    void intercept_tokenCachedAcrossMultipleRequests() {
        tokenServer.stubFor(get(urlEqualTo(TOKEN_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"token\":\"j2-shared\"}")));

        apiServer.stubFor(get(urlEqualTo(API_ENDPOINT))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withStatus(200)
                        .withBody("{\"data\":\"ok\"}")));

        // Three separate API calls
        List.of(1, 2, 3).forEach(i ->
                apiRestClient.get().uri(API_ENDPOINT).retrieve().body(String.class));

        // Token fetched only once — cache served the other two
        tokenServer.verify(1, getRequestedFor(urlEqualTo(TOKEN_ENDPOINT)));
        apiServer.verify(3, getRequestedFor(urlEqualTo(API_ENDPOINT)));
    }
}
