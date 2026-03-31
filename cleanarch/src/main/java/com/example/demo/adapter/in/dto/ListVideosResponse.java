package com.example.demo.adapter.in.dto;

import com.example.demo.usecase.ports.in.dto.VideoDto;

import java.util.List;

public record ListVideosResponse(List<VideoDto> videos) {}
