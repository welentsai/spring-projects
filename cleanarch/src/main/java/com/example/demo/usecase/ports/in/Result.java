package com.example.demo.usecase.ports.in;

public class Result<T> implements Output {
    private final String returnCode;
    private final String errorMessage;
    private final T data;

    protected Result(String returnCode, String errorMessage, T data) {
        this.returnCode = returnCode;
        this.errorMessage = errorMessage;
        this.data = data;
    }

    public static <T> Result<T> success(T data) {
        return new Result<>("SUCCESS", null, data);
    }

    public static <T> Result<T> failure(String errorMessage) {
        return new Result<>("FAILURE", errorMessage, null);
    }

    public static <T> Result<T> failure(String returnCode, String errorMessage) {
        return new Result<>(returnCode, errorMessage, null);
    }

    public String getReturnCode() {
        return returnCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public T getData() {
        return data;
    }

    public boolean isSuccess() {
        return "SUCCESS".equals(returnCode);
    }

    public boolean isFailure() {
        return !isSuccess();
    }
}
