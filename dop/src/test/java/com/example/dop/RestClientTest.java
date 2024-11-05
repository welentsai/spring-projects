package com.example.dop;

import com.example.dop.util.QueryParams;
import com.example.dop.util.RestClientUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        var response = restClientUtils.get("/api/v1/search", String.class, params1);
        System.out.println(response);
    }
}
