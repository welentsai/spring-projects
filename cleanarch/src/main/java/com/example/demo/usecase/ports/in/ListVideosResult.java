package com.example.demo.usecase.ports.in;

import com.example.demo.usecase.ports.in.dto.VideoDto;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import java.util.List;

@JsonPropertyOrder({"returnCode", "errorMessage", "data", "videos", "success", "failure"})
public class ListVideosResult extends Result<List<VideoDto>> {

    private ListVideosResult(String returnCode, String errorMessage, List<VideoDto> data) {
        super(returnCode, errorMessage, data);
    }

    public static ListVideosResult success(List<VideoDto> videos) {
        return new ListVideosResult("SUCCESS", null, videos);
    }

    public static ListVideosResult failure(String errorMessage) {
        return new ListVideosResult("FAILURE", errorMessage, null);
    }

    public static ListVideosResult failure(String returnCode, String errorMessage) {
        return new ListVideosResult(returnCode, errorMessage, null);
    }

    public List<VideoDto> getVideos() {
        return getData();
    }
}
