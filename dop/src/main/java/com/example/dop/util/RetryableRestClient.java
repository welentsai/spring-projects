package com.example.dop.util;

import com.example.dop.util.exception.RetryableRestClientException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.timelimiter.TimeLimiter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class RetryableRestClient {
    private static final Logger logger = LoggerFactory.getLogger(RetryableRestClient.class);

    private final RestClient restClient;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final TimeLimiter timeLimiter;

    public RetryableRestClient(
            RestClient restClient,
            Retry retry,
            CircuitBreaker circuitBreaker,
            TimeLimiter timeLimiter) {

        this.restClient = restClient;
        this.retry = retry;
        this.circuitBreaker = circuitBreaker;
        this.timeLimiter = timeLimiter;

        registerEventListeners(retry);
    }

    public void registerEventListener(Consumer<Retry> retry) {
        retry.accept(this.retry);
    }

    // GET with resilience patterns
    public <T> T get(String uri, Class<T> responseType, Object... uriVariables) {
        return executeWithResilience(
                () -> {
                    logger.info("Making GET request to: {}", uri);
                    return restClient.get().uri(uri, uriVariables).retrieve().body(responseType);
                },
                "GET-" + uri.replaceAll("[{}]", ""));
    }

    // GET with custom error handling
    public <T> T getWithErrorHandler(
            String uri,
            Class<T> responseType,
            Function<HttpStatusCode, T> errorHandler,
            Object... uriVariables) {
        return executeWithResilience(
                () -> {
                    logger.debug("Making GET request with error handler to: {}", uri);
                    try {
                        return restClient
                                .get()
                                .uri(uri, uriVariables)
                                .retrieve()
                                .body(responseType);
                    } catch (HttpClientErrorException e) {
                        if (e.getStatusCode().is4xxClientError() && errorHandler != null) {
                            logger.debug(
                                    "Applying error handler for status: {}", e.getStatusCode());
                            return errorHandler.apply(e.getStatusCode());
                        }
                        throw e;
                    }
                },
                "GET-" + uri.replaceAll("[{}]", ""));
    }

    // POST with resilience patterns
    public <T, R> R post(String uri, T requestBody, Class<R> responseType, Object... uriVariables) {
        return executeWithResilience(
                () -> {
                    logger.debug("Making POST request to: {}", uri);
                    return restClient
                            .post()
                            .uri(uri, uriVariables)
                            .body(requestBody)
                            .retrieve()
                            .body(responseType);
                },
                "POST-" + uri.replaceAll("[{}]", ""));
    }

    // PUT with resilience patterns
    public <T, R> R put(String uri, T requestBody, Class<R> responseType, Object... uriVariables) {
        return executeWithResilience(
                () -> {
                    logger.debug("Making PUT request to: {}", uri);
                    return restClient
                            .put()
                            .uri(uri, uriVariables)
                            .body(requestBody)
                            .retrieve()
                            .body(responseType);
                },
                "PUT-" + uri.replaceAll("[{}]", ""));
    }

    // DELETE with resilience patterns
    public void delete(String uri, Object... uriVariables) {
        executeWithResilience(
                () -> {
                    logger.debug("Making DELETE request to: {}", uri);
                    restClient.delete().uri(uri, uriVariables).retrieve().toBodilessEntity();
                    return null;
                },
                "DELETE-" + uri.replaceAll("[{}]", ""));
    }

    // Core method with all resilience patterns
    private <T> T executeWithResilience(Supplier<T> operation, String operationName) {
        Supplier<T> decoratedSupplier =
                CircuitBreaker.decorateSupplier(
                        circuitBreaker, Retry.decorateSupplier(retry, operation));

        // Apply timeout with TimeLimiter
        Supplier<CompletableFuture<T>> futureSupplier =
                () -> CompletableFuture.supplyAsync(decoratedSupplier);

        Callable<T> decoratedCallable =
                TimeLimiter.decorateFutureSupplier(timeLimiter, futureSupplier);

        try {
            return decoratedCallable.call();
        } catch (Exception e) {
            String errorMsg =
                    String.format(
                            "Operation %s failed after applying all resilience patterns: %s",
                            operationName, e.getMessage());
            logger.error(errorMsg);
            throw new RetryableRestClientException(errorMsg, e);
        }
    }

    private void registerEventListeners(Retry retry) {
        // Optional: Attach event listeners for logging retry attempts
        retry.getEventPublisher()
                .onRetry(
                        event ->
                                logger.warn(
                                        "Retry attempt for {}: {}",
                                        event.getName(),
                                        event.getLastThrowable().getMessage()))
                .onSuccess(
                        event ->
                                logger.info(
                                        "Retry success for {} after {} attempts",
                                        event.getName(),
                                        event.getNumberOfRetryAttempts() + 1))
                .onError(
                        event ->
                                logger.error(
                                        "Retry failed for {} after {} attempts: {}",
                                        event.getName(),
                                        event.getNumberOfRetryAttempts() + 1,
                                        event.getLastThrowable().getMessage()));
    }
}
