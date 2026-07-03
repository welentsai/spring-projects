package com.example.demo.framework.config;

import com.example.demo.adapter.in.mcp.CitiesToolAdapter;
import com.example.demo.adapter.in.mcp.VideoToolAdapter;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.ListVideosUseCase;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class McpConfig {

    @Bean
    public CitiesToolAdapter citiesToolAdapter(FindCitiesUseCase findCitiesUseCase) {
        return new CitiesToolAdapter(findCitiesUseCase);
    }

    @Bean
    public VideoToolAdapter videoToolAdapter(ListVideosUseCase listVideosUseCase) {
        return new VideoToolAdapter(listVideosUseCase);
    }

    @Bean
    public ToolCallbackProvider mcpToolCallbackProvider(
            CitiesToolAdapter citiesToolAdapter, VideoToolAdapter videoToolAdapter) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(citiesToolAdapter, videoToolAdapter)
                .build();
    }
}
