package com.example.demo.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.example.demo.adapter.in.dto.ListVideosResponse;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.ListVideosInput;
import com.example.demo.usecase.ports.in.ListVideosResult;
import com.example.demo.usecase.ports.in.ListVideosUseCase;
import com.example.demo.usecase.ports.in.dto.VideoDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class VideoToolAdapterTest {

    @Mock
    private ListVideosUseCase listVideosUseCase;

    @InjectMocks
    private VideoToolAdapter videoToolAdapter;

    @Test
    void listVideosReturnsVideosFromUseCase() {
        List<VideoDto> videos = List.of(new VideoDto("a.mp4", "videos", 42L, "2026-01-01"));
        when(listVideosUseCase.execute(any(ListVideosInput.class)))
                .thenReturn(ListVideosResult.success(videos));

        ListVideosResponse response = videoToolAdapter.listVideos("videos");

        assertThat(response.videos()).containsExactlyElementsOf(videos);
    }

    @Test
    void listVideosRejectsBlankBucketName() {
        assertThatThrownBy(() -> videoToolAdapter.listVideos(" "))
                .isInstanceOf(BadRequestException.class);

        verifyNoInteractions(listVideosUseCase);
    }
}
