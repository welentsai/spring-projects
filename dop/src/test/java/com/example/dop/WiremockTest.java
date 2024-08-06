package com.example.dop;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static io.restassured.RestAssured.given;

import static com.github.tomakehurst.wiremock.client.WireMock.*;

public class WiremockTest {
    private WireMockServer wireMockServer;

    @BeforeEach
    public void setUp() {
        System.out.println("Setup wiremock server !");
        wireMockServer = new WireMockServer(wireMockConfig().port(8090).usingFilesUnderClasspath("src/test/resources/wiremock"));
        wireMockServer.start();
    }

    @AfterEach
    public void teardown() {
        wireMockServer.stop();
    }

    private void setupStub() {
        wireMockServer.stubFor(get(urlEqualTo("/an/endpoint"))
                .willReturn(aResponse().withHeader("Content-Type", "text/plain")
                        .withStatus(200)
                        .withBodyFile("glossary.json")));
    }

    @Test
    public void testStatusCodePositive() {
        // Arrange
        setupStub();

        // Act
        given().
                when().
                get("http://localhost:8090/an/endpoint").
                then().
                assertThat().statusCode(200);
    }

}
