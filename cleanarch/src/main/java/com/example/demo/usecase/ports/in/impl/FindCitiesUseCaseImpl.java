package com.example.demo.usecase.ports.in.impl;

import com.example.demo.domain.model.City;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.dto.CityDto;

import java.util.List;

public class FindCitiesUseCaseImpl implements FindCitiesUseCase {
    @Override
    public FindCitiesResult execute(FindCitiesInput input) {
        try {
            // Simulate business logic - in real implementation this would call repository
            List<City> cities = List.of(
                new City("1", "Taipei", "Taiwan"),
                new City("2", "Tokyo", "Japan")
            );
            
            // Map domain objects to DTOs
            List<CityDto> cityDtos = cities.stream()
                .map(city -> new CityDto(city.id(), city.name(), city.country()))
                .toList();
            
            return FindCitiesResult.success(cityDtos);
        } catch (Exception e) {
            return FindCitiesResult.failure("INTERNAL_ERROR", "Failed to retrieve cities: " + e.getMessage());
        }
    }
}
