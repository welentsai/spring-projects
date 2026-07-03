package com.example.demo.adapter.in.mcp;

import com.example.demo.adapter.in.dto.FindCitiesRequest;
import com.example.demo.adapter.in.dto.FindCitiesResponse;
import com.example.demo.adapter.in.mapper.CitiesRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;

import org.springframework.ai.tool.annotation.Tool;

public class CitiesToolAdapter {

    private final FindCitiesUseCase findCitiesUseCase;

    public CitiesToolAdapter(FindCitiesUseCase findCitiesUseCase) {
        this.findCitiesUseCase = findCitiesUseCase;
    }

    @Tool(
            name = "find_cities",
            description = "Find all cities. Returns id, name and country for each city."
                    + " The query is routed to the primary or secondary datasource at runtime.")
    public FindCitiesResponse findCities() {
        return new FindCitiesRequest()
                .validate()
                .map(CitiesRequestMapper::toInput)
                .map(findCitiesUseCase::execute)
                .map(CitiesRequestMapper::toResponse)
                .orElseThrow(BadRequestException::new);
    }
}
