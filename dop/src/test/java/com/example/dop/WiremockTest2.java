package com.example.dop;

import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static io.restassured.RestAssured.given;

@WireMockTest(httpPort = 8090)
public class WiremockTest2 {

    @Test
    public void test(WireMockRuntimeInfo runtimeInfo) {
        // Arrange
        stubFor(get("/api/data")
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"message\":\"Hello from WireMock!\"}")
                        .withStatus(200)));

        // Act
        given().
                when().
                get("http://localhost:8090/api/data").
                then().
                assertThat().statusCode(200);
    }

//    @Test
//    public void test2(WireMockRuntimeInfo runtimeInfo) {
//
//        // Arrange
//        stubFor(get(urlEqualTo("/an/endpoint"))
//                .willReturn(aResponse().withHeader("Content-Type", "text/plain")
//                        .withStatus(200)
//                        .withBodyFile("glossary.json")));
//
//        // Act
//        given().
//                when().
//                get("http://localhost:8090/an/endpoint").
//                then().
//                assertThat().statusCode(200);
//    }
}
