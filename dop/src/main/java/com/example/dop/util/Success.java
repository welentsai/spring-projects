package com.example.dop.util;

public final class Success<T> implements Try<T> {
    private T result;

    Success(T result) {
        this.result = result;
    }

    @Override
    public T getResult() {
        return result;
    }

    @Override
    public Throwable getError() {
        return new RuntimeException("Invalid invocation");
    }
}
