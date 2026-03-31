package com.example.demo.usecase.ports.out.gateway;

import com.example.demo.usecase.ports.in.dto.VideoDto;

import java.util.List;

public record VideoStorageOutput(List<VideoDto> videos) {}
