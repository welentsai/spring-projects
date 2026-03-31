package com.example.demo.adapter.in;

import com.example.demo.adapter.in.dto.ListVideosRequest;
import com.example.demo.adapter.in.dto.ListVideosResponse;
import com.example.demo.adapter.in.mapper.VideoRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.framework.config.VideoConfig;
import com.example.demo.usecase.ports.in.ListVideosUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/videos")
public class VideoController {

    private final ListVideosUseCase listVideosUseCase;
    private final VideoConfig videoConfig;

    VideoController(ListVideosUseCase listVideosUseCase, VideoConfig videoConfig) {
        this.listVideosUseCase = listVideosUseCase;
        this.videoConfig = videoConfig;
    }

    @GetMapping
    public ResponseEntity<ListVideosResponse> getAllVideos() {
        ListVideosResponse response = new ListVideosRequest(videoConfig.getBucketName())
                .validate()
                .map(VideoRequestMapper::toInput)
                .map(listVideosUseCase::execute)
                .map(VideoRequestMapper::toResponse)
                .orElseThrow(BadRequestException::new);

        return ResponseEntity.ok(response);
    }
}
