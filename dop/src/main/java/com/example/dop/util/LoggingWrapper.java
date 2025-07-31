package com.example.dop.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

public class LoggingWrapper {
    private static final Logger logger = LoggerFactory.getLogger(LoggingWrapper.class);

    /**
     * Functional interface that allows checked exceptions
     */
    @FunctionalInterface
    public interface ThrowingFunction<T, R> {
        R apply(T t) throws Exception;
    }

    /**
     * Functional interface for no-input functions that allows checked exceptions
     */
    @FunctionalInterface
    public interface ThrowingSupplier<R> {
        R get() throws Exception;
    }

    /**
     * Enhanced method that handles checked exceptions by wrapping them in RuntimeException
     */
    public static <T, R> Function<T, R> withLoggingAndExceptionHandling(String functionName, ThrowingFunction<T, R> function) {
        return t -> {
            try {
                return getResp(functionName, function, t);
            } catch (RuntimeException e) {
                logger.error("[{}] Runtime exception occurred: {}", functionName, e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                logger.error("[{}] Checked exception occurred: {}", functionName, e.getMessage(), e);
                throw new RuntimeException("Wrapped exception: " + e.getMessage(), e);
            }
        };
    }

    /**
     * Enhanced method for no-input functions (Supplier-like) that handles checked exceptions
     */
    public static <R> Supplier<R> withLoggingAndExceptionHandling(String functionName, ThrowingSupplier<R> supplier) {
        return () -> {
            try {
                logger.info("executing [{}]", functionName);
                R resp = supplier.get();
                logger.info("[{}] response: {}", functionName, JsonUtils.toJson(resp));
                return resp;
            } catch (RuntimeException e) {
                logger.error("[{}] Runtime exception occurred: {}", functionName, e.getMessage(), e);
                throw e;
            } catch (Exception e) {
                logger.error("[{}] Checked exception occurred: {}", functionName, e.getMessage(), e);
                throw new RuntimeException("Wrapped exception: " + e.getMessage(), e);
            }
        };
    }


    /**
     * Method that returns Optional instead of throwing exceptions
     */
    public static <T, R> Function<T, Optional<R>> withLoggingAndExceptionHandlingSafe(String functionName, ThrowingFunction<T, R> function) {
        return t -> {
            try {
                return Optional.of(getResp(functionName, function, t));
            } catch (RuntimeException e) {
                logger.error("[{}] Runtime exception occurred: {}", functionName, e.getMessage(), e);
                return Optional.empty();
            } catch (Exception e) {
                logger.error("[{}] Checked exception occurred: {}", functionName, e.getMessage(), e);
                return Optional.empty();
            }
        };
    }

    private static <T, R> R getResp(String functionName, ThrowingFunction<T, R> function, T t) throws Exception {
        logger.info("[{}] request: {}", functionName, t);
        R resp = function.apply(t);
        logger.info("[{}] response: {}", functionName, JsonUtils.toJson(resp));
        return resp;
    }
}
