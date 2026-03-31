package com.example.demo.usecase.ports.in;

public class ListVideosInput implements Input {
    private final String bucketName;

    public ListVideosInput(String bucketName) {
        this.bucketName = bucketName;
    }

    public String getBucketName() {
        return bucketName;
    }
}
