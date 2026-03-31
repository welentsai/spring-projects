package com.example.demo.adapter.out.gateway;

import com.example.demo.usecase.ports.in.dto.VideoDto;
import com.example.demo.usecase.ports.out.gateway.VideoStorageGateway;
import com.example.demo.usecase.ports.out.gateway.VideoStorageInput;
import com.example.demo.usecase.ports.out.gateway.VideoStorageOutput;

import io.minio.ListObjectsArgs;
import io.minio.MinioClient;
import io.minio.Result;
import io.minio.messages.Item;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class VideoStorageGatewayImpl implements VideoStorageGateway {
    private static final Logger logger = LoggerFactory.getLogger(VideoStorageGatewayImpl.class);

    private final MinioClient minioClient;

    public VideoStorageGatewayImpl(MinioClient minioClient) {
        this.minioClient = minioClient;
    }

    @Override
    public VideoStorageOutput execute(VideoStorageInput input) {
        List<VideoDto> videos = new ArrayList<>();

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder().bucket(input.bucketName()).build());

        for (Result<Item> result : results) {
            try {
                Item item = result.get();
                videos.add(new VideoDto(
                        item.objectName(),
                        input.bucketName(),
                        item.size(),
                        item.lastModified() != null ? item.lastModified().toString() : null));
            } catch (Exception e) {
                logger.warn("Failed to read object metadata", e);
            }
        }

        return new VideoStorageOutput(videos);
    }
}
