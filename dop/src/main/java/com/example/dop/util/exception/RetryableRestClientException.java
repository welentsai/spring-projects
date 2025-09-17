package com.example.dop.util.exception;

public class RetryableRestClientException extends RuntimeException {
    public RetryableRestClientException(String message) {
        super(message);
    }

    public RetryableRestClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
