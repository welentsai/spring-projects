package com.example.demo.adapter.in.mapper;

import com.example.demo.adapter.in.dto.ListVideosRequest;
import com.example.demo.adapter.in.dto.ListVideosResponse;
import com.example.demo.usecase.ports.in.ListVideosInput;
import com.example.demo.usecase.ports.in.ListVideosResult;

public class VideoRequestMapper {

    private VideoRequestMapper() {}

    public static ListVideosInput toInput(ListVideosRequest request) {
        return new ListVideosInput(request.bucketName());
    }

    public static ListVideosResponse toResponse(ListVideosResult result) {
        return new ListVideosResponse(result.getVideos());
    }
}
