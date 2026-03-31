package com.example.demo.adapter.in.dto;

import java.util.Optional;

public record ListVideosRequest(String bucketName) {

    public Optional<ListVideosRequest> validate() {
        if (bucketName == null || bucketName.isBlank()) return Optional.empty();
        return Optional.of(this);
    }
}
