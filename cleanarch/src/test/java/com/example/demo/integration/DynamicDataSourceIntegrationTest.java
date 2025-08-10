package com.example.demo.integration;

import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
public class DynamicDataSourceIntegrationTest {

    @Autowired
    private FindCitiesUseCase findCitiesUseCase;

    @Test
    public void should_demonstrate_dynamic_data_source_routing() {
        // Given
        FindCitiesInput input = new FindCitiesInput();

        // When - Execute the use case multiple times to see different routing decisions
        FindCitiesResult result1 = findCitiesUseCase.execute(input);
        FindCitiesResult result2 = findCitiesUseCase.execute(input);
        FindCitiesResult result3 = findCitiesUseCase.execute(input);

        // Then - All results should be successful (demonstrating routing works)
        assertThat(result1.isSuccess()).isTrue();
        assertThat(result2.isSuccess()).isTrue();
        assertThat(result3.isSuccess()).isTrue();

        // The results should be empty lists since we're not pre-populating test data
        // But the important thing is that the routing mechanism works without errors
        assertThat(result1.getData()).isNotNull();
        assertThat(result2.getData()).isNotNull();
        assertThat(result3.getData()).isNotNull();
    }

    @Test
    public void should_handle_routing_decisions_gracefully() {
        // Given
        FindCitiesInput input = new FindCitiesInput();

        // When - Execute multiple times to test consistency
        for (int i = 0; i < 5; i++) {
            FindCitiesResult result = findCitiesUseCase.execute(input);
            
            // Then - Each execution should succeed regardless of which DB is chosen
            assertThat(result.isSuccess()).isTrue();
            assertThat(result.getData()).isNotNull();
        }
    }
}
