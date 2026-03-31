package com.example.demo.framework.di;

import com.example.demo.adapter.out.gateway.VideoStorageGatewayImpl;
import com.example.demo.usecase.ports.in.ListVideosUseCase;
import com.example.demo.usecase.ports.in.impl.ListVideosUseCaseImpl;
import com.example.demo.usecase.ports.out.gateway.PutLogGateway;
import com.example.demo.usecase.ports.out.gateway.VideoStorageGateway;

import io.minio.MinioClient;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BeanInjection {
    @Bean
    public VideoStorageGateway videoStorageGateway(MinioClient minioClient) {
        return new VideoStorageGatewayImpl(minioClient);
    }

    @Bean
    public ListVideosUseCase listVideosUseCase(
            VideoStorageGateway videoStorageGateway, PutLogGateway putLogGateway) {
        return new ListVideosUseCaseImpl(videoStorageGateway, putLogGateway);
    }
}
