package com.example.demo.adapter.in.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.demo.adapter.in.dto.FindCitiesResponse;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.dto.CityDto;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class CitiesToolAdapterTest {

    @Mock
    private FindCitiesUseCase findCitiesUseCase;

    @InjectMocks
    private CitiesToolAdapter citiesToolAdapter;

    @Test
    void findCitiesReturnsCitiesFromUseCase() {
        List<CityDto> cities = List.of(new CityDto("1", "Taipei", "Taiwan"));
        when(findCitiesUseCase.execute(any(FindCitiesInput.class)))
                .thenReturn(FindCitiesResult.success(cities));

        FindCitiesResponse response = citiesToolAdapter.findCities();

        assertThat(response.cities()).containsExactlyElementsOf(cities);
    }
}
