package com.example.demo.adapter.in.mcp;

import com.example.demo.adapter.in.dto.ListVideosRequest;
import com.example.demo.adapter.in.dto.ListVideosResponse;
import com.example.demo.adapter.in.mapper.VideoRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.ListVideosUseCase;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

public class VideoToolAdapter {

    private final ListVideosUseCase listVideosUseCase;

    public VideoToolAdapter(ListVideosUseCase listVideosUseCase) {
        this.listVideosUseCase = listVideosUseCase;
    }

    @Tool(
            name = "list_videos",
            description = "List the videos stored in a MinIO bucket."
                    + " Returns name, bucket, size and last-modified timestamp for each video.")
    public ListVideosResponse listVideos(
            @ToolParam(description = "Name of the MinIO bucket to list, e.g. 'videos'")
                    String bucketName) {
        return new ListVideosRequest(bucketName)
                .validate()
                .map(VideoRequestMapper::toInput)
                .map(listVideosUseCase::execute)
                .map(VideoRequestMapper::toResponse)
                .orElseThrow(BadRequestException::new);
    }
}
