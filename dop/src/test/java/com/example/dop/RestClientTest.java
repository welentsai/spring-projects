package com.example.dop;

import com.example.dop.util.QueryParams;
import com.example.dop.util.RestClientUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.function.Supplier;

public class RestClientTest {

    private RestClientUtils restClientUtils;

    @BeforeEach
    public void setUp() {
        restClientUtils = new RestClientUtils("https://hn.algolia.com");
    }

    @Test
    void test() {
        // Simple query parameters
        QueryParams params1 = QueryParams.builder()
                .add("query", "react")
                .build();

        createDefaultRestClientSupplier("https://hn.algolia.com");

        var response = restClientUtils.get(createDefaultRestClientSupplier(
                        "https://hn.algolia.com"),
                "/api/v1/search", String.class, params1);
        System.out.println(response);
    }


    private Supplier<RestClient> createDefaultRestClientSupplier(String baseUrl) {
        return () -> RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
