package com.example.demo.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.framework.di.dynamicdatasource.DataSourceContextHolder;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingGateway;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingInput;
import com.example.demo.usecase.ports.out.gateway.InlineRoutingOutput;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
public class DynamicDataSourceIntegrationTest {

    @Autowired private FindCitiesUseCase findCitiesUseCase;

    @MockBean private InlineRoutingGateway inlineRoutingGateway;

    @AfterEach
    void cleanup() {
        // Ensure context is cleaned up after each test
        DataSourceContextHolder.clearDataSourceKey();
    }

    @Test
    public void should_use_primary_data_source_when_routing_gateway_returns_primary() {
        // Given
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("PRIMARY"));

        FindCitiesInput input = new FindCitiesInput();

        // When
        FindCitiesResult result = findCitiesUseCase.execute(input);

        // Then - Should succeed with primary database (which has the table structure)
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.getData()).isNotNull();
        assertThat(result.getData()).isEmpty(); // No data inserted, but structure exists
    }

    @Test
    public void should_handle_secondary_data_source_gracefully() {
        // Given
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("SECONDARY"));

        FindCitiesInput input = new FindCitiesInput();

        // When
        FindCitiesResult result = findCitiesUseCase.execute(input);

        // Then - Should fail gracefully when secondary DB doesn't have table structure
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReturnCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(result.getErrorMessage()).contains("Failed to retrieve cities");
    }

    @Test
    public void should_demonstrate_routing_context_management() {
        // Given
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("PRIMARY"));

        FindCitiesInput input = new FindCitiesInput();

        // When
        findCitiesUseCase.execute(input);

        // Then - Context should be cleared after execution
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
    }

    @Test
    public void should_handle_multiple_consecutive_calls_with_different_routing() {
        // Given - Alternate between PRIMARY and SECONDARY
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("PRIMARY"))
                .thenReturn(new InlineRoutingOutput("SECONDARY"))
                .thenReturn(new InlineRoutingOutput("PRIMARY"));

        FindCitiesInput input = new FindCitiesInput();

        // When & Then
        // First call - PRIMARY (should succeed)
        FindCitiesResult result1 = findCitiesUseCase.execute(input);
        assertThat(result1.isSuccess()).isTrue();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull(); // Context cleared

        // Second call - SECONDARY (should fail gracefully)
        FindCitiesResult result2 = findCitiesUseCase.execute(input);
        assertThat(result2.isSuccess()).isFalse();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull(); // Context cleared

        // Third call - PRIMARY (should succeed again)
        FindCitiesResult result3 = findCitiesUseCase.execute(input);
        assertThat(result3.isSuccess()).isTrue();
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull(); // Context cleared
    }

    @Test
    public void should_handle_unknown_data_source_key() {
        // Given
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("UNKNOWN"));

        FindCitiesInput input = new FindCitiesInput();

        // When
        FindCitiesResult result = findCitiesUseCase.execute(input);

        // Then - Should default to SECONDARY and fail gracefully
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getReturnCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull(); // Context cleared
    }

    @Test
    public void should_demonstrate_thread_safety() throws InterruptedException {
        // Given
        when(inlineRoutingGateway.execute(any(InlineRoutingInput.class)))
                .thenReturn(new InlineRoutingOutput("PRIMARY"));

        FindCitiesInput input = new FindCitiesInput();

        // When - Execute in multiple threads
        Thread[] threads = new Thread[5];
        FindCitiesResult[] results = new FindCitiesResult[5];

        for (int i = 0; i < 5; i++) {
            final int index = i;
            threads[i] =
                    new Thread(
                            () -> {
                                results[index] = findCitiesUseCase.execute(input);
                            });
            threads[i].start();
        }

        // Wait for all threads to complete
        for (Thread thread : threads) {
            thread.join();
        }

        // Then - All should succeed and context should be clean
        for (FindCitiesResult result : results) {
            assertThat(result.isSuccess()).isTrue();
        }
        assertThat(DataSourceContextHolder.getDataSourceKey()).isNull();
    }
}
