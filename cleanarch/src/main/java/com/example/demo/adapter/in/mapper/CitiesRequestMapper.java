package com.example.demo.adapter.in.mapper;

import com.example.demo.adapter.in.dto.FindCitiesRequest;
import com.example.demo.adapter.in.dto.FindCitiesResponse;
import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;

public class CitiesRequestMapper {

    private CitiesRequestMapper() {}

    public static FindCitiesInput toInput(FindCitiesRequest request) {
        return new FindCitiesInput();
    }

    public static FindCitiesResponse toResponse(FindCitiesResult result) {
        return new FindCitiesResponse(result.getCities());
    }
}
