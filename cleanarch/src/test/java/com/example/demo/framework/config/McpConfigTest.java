package com.example.demo.framework.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.ListVideosUseCase;

import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.ToolCallbackProvider;

import java.util.Arrays;

class McpConfigTest {

    @Test
    void registersFindCitiesAndListVideosAsMcpTools() {
        McpConfig config = new McpConfig();

        ToolCallbackProvider provider = config.mcpToolCallbackProvider(
                config.citiesToolAdapter(mock(FindCitiesUseCase.class)),
                config.videoToolAdapter(mock(ListVideosUseCase.class)));

        assertThat(Arrays.stream(provider.getToolCallbacks())
                        .map(ToolCallback::getToolDefinition)
                        .map(definition -> definition.name()))
                .containsExactlyInAnyOrder("find_cities", "list_videos");
    }
}
