package com.example.dop.util;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.function.Function;
import java.util.function.Supplier;

public class RestClientUtils {

    private final RestClient restClient;

    public RestClientUtils(RestClient restClient) {
        this.restClient = restClient;
    }

    public RestClientUtils(String baseUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /**
     * Generic GET request
     *
     * @param uri          The endpoint URI
     * @param responseType The expected response type
     * @param uriVariables Optional URI variables
     * @return HttpResponse wrapper containing the response
     */
    public <T> T get(Supplier<RestClient> restClientSupplier, String uri, Class<T> responseType, Object... uriVariables) {
        return executeRequest(restClientSupplier, builder ->
                        builder.get()
                                .uri(uri, uriVariables)
                , responseType);
    }

    /**
     * Generic GET request
     *
     * @param uri          The endpoint URI
     * @param responseType The expected response type
     * @param uriVariables Optional URI variables
     * @return HttpResponse wrapper containing the response
     */
    public <T> T get(Supplier<RestClient> restClientSupplier, String uri, Class<T> responseType, QueryParams queryParams, Object... uriVariables) {
        return executeRequest(restClientSupplier,
                builder ->
                        builder.get()
                                .uri(uriBuilder -> uriBuilder
                                        .path(uri)
                                        .queryParams(queryParams.toMultiValueMap())
                                        .build(uriVariables)
                                )
                , responseType);
    }

    /**
     * Generic POST request
     *
     * @param uri          The endpoint URI
     * @param body         The request body
     * @param responseType The expected response type
     * @param uriVariables Optional URI variables
     * @return HttpResponse wrapper containing the response
     */
    public <T, R> R post(Supplier<RestClient> restClientSupplier, String uri, T body, Class<R> responseType, Object... uriVariables) {
        return executeRequest(
                restClientSupplier,
                builder ->
                        builder.post()
                                .uri(uri, uriVariables)
                                .body(body)
                , responseType);
    }

    /**
     * Generic request execution with response type class
     */
    private <T> T executeRequest(
            Supplier<RestClient> restClientSupplier,
            Function<RestClient, RestClient.RequestHeadersSpec<?>> requestBuilder,
            Class<T> responseType) {
        try {
            var response = requestBuilder.apply(restClientSupplier.get())
                    .retrieve()
                    .body(responseType);

            return response;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
//            return handleException(e);
        }
    }
}
