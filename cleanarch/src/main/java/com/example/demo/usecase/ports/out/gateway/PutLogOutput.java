package com.example.demo.usecase.ports.out.gateway;

import java.util.Map;

public record PutLogOutput(Map<String, Object> logs) {

    public static PutLogOutput of(Map<String, Object> logs) {
        return new PutLogOutput(logs);
    }
}
