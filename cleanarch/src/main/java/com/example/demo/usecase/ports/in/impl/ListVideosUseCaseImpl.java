package com.example.demo.usecase.ports.in.impl;

import com.example.demo.usecase.ports.in.ListVideosInput;
import com.example.demo.usecase.ports.in.ListVideosResult;
import com.example.demo.usecase.ports.in.ListVideosUseCase;
import com.example.demo.usecase.ports.out.gateway.PutLogGateway;
import com.example.demo.usecase.ports.out.gateway.PutLogInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageGateway;
import com.example.demo.usecase.ports.out.gateway.VideoStorageInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageOutput;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ListVideosUseCaseImpl implements ListVideosUseCase {
    private static final Logger logger = LoggerFactory.getLogger(ListVideosUseCaseImpl.class);

    private final VideoStorageGateway videoStorageGateway;
    private final PutLogGateway putLogGateway;

    public ListVideosUseCaseImpl(
            VideoStorageGateway videoStorageGateway, PutLogGateway putLogGateway) {
        this.videoStorageGateway = videoStorageGateway;
        this.putLogGateway = putLogGateway;
    }

    @Override
    public ListVideosResult execute(ListVideosInput input) {
        try {
            logger.info("Listing videos from bucket: {}", input.getBucketName());
            putLogGateway.execute(
                    new PutLogInput("Listing videos from bucket", input.getBucketName()));

            VideoStorageOutput output =
                    videoStorageGateway.execute(new VideoStorageInput(input.getBucketName()));

            logger.info("Found {} videos", output.videos().size());
            return ListVideosResult.success(output.videos());
        } catch (Exception e) {
            logger.error("Failed to list videos", e);
            putLogGateway.execute(new PutLogInput("Failed to list videos", e.getMessage()));
            return ListVideosResult.failure(
                    "INTERNAL_ERROR", "Failed to list videos: " + e.getMessage());
        }
    }
}
