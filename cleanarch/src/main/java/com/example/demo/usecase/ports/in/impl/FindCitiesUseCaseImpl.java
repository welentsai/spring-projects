package com.example.demo.usecase.ports.in.impl;

import com.example.demo.domain.model.City;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import com.example.demo.usecase.ports.in.dto.CityDto;
import com.example.demo.usecase.ports.out.entity.CityJpaEntity;
import com.example.demo.usecase.ports.out.repository.CitiesQueryRepository;

import java.util.List;

public class FindCitiesUseCaseImpl implements FindCitiesUseCase {
    
    private final CitiesQueryRepository citiesQueryRepository;
    
    public FindCitiesUseCaseImpl(CitiesQueryRepository citiesQueryRepository) {
        this.citiesQueryRepository = citiesQueryRepository;
    }
    
    @Override
    public FindCitiesResult execute(FindCitiesInput input) {
        try {
            // Use the repository to get all cities
            List<CityJpaEntity> cityEntities = citiesQueryRepository.findAll();
            
            // Map JPA entities to DTOs
            List<CityDto> cityDtos = cityEntities.stream()
                .map(entity -> new CityDto(entity.getId(), entity.getName(), entity.getCountry()))
                .toList();
            
            return FindCitiesResult.success(cityDtos);
        } catch (Exception e) {
            return FindCitiesResult.failure("INTERNAL_ERROR", "Failed to retrieve cities: " + e.getMessage());
        }
    }
}
