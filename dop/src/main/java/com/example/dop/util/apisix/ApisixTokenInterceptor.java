package com.example.dop.util.apisix;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.support.HttpRequestWrapper;

import java.io.IOException;

public class ApisixTokenInterceptor implements ClientHttpRequestInterceptor {

    private final ApisixTokenProvider tokenProvider;
    private final String tokenHeaderName;
    private final String tokenHeaderPrefix;

    public ApisixTokenInterceptor(
            ApisixTokenProvider tokenProvider,
            String tokenHeaderName,
            String tokenHeaderPrefix) {
        this.tokenProvider = tokenProvider;
        this.tokenHeaderName = tokenHeaderName;
        this.tokenHeaderPrefix = tokenHeaderPrefix;
    }

    @Override
    public ClientHttpResponse intercept(
            HttpRequest request, byte[] body, ClientHttpRequestExecution execution)
            throws IOException {

        ClientHttpResponse response =
                execution.execute(addTokenHeader(request, tokenProvider.getToken()), body);

        if (response.getStatusCode().value() == 401) {
            // J2 token expired: release the stale response connection, refresh, retry once
            response.close();
            tokenProvider.invalidate();
            return execution.execute(addTokenHeader(request, tokenProvider.getToken()), body);
        }

        return response;
    }

    private HttpRequest addTokenHeader(HttpRequest original, String tokenValue) {
        return new HttpRequestWrapper(original) {
            @Override
            public HttpHeaders getHeaders() {
                HttpHeaders headers = new HttpHeaders();
                headers.putAll(super.getHeaders());
                headers.set(tokenHeaderName, buildHeaderValue(tokenValue));
                return headers;
            }
        };
    }

    // Normalizes prefix so "Bearer", "Bearer ", and "" all produce the correct header value.
    private String buildHeaderValue(String tokenValue) {
        String trimmed = tokenHeaderPrefix.strip();
        return trimmed.isBlank() ? tokenValue : trimmed + " " + tokenValue;
    }
}
