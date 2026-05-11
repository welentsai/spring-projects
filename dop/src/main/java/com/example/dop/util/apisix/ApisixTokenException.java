package com.example.dop.util.apisix;

public class ApisixTokenException extends RuntimeException {

    public ApisixTokenException(String message) {
        super(message);
    }

    public ApisixTokenException(String message, Throwable cause) {
        super(message, cause);
    }
}
