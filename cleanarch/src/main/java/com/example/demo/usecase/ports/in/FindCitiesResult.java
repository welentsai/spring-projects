package com.example.demo.usecase.ports.in;

import com.example.demo.usecase.ports.in.dto.CityDto;

import java.util.List;

public class FindCitiesResult extends Result<List<CityDto>> {

    private FindCitiesResult(String returnCode, String errorMessage, List<CityDto> data) {
        super(returnCode, errorMessage, data);
    }

    public static FindCitiesResult success(List<CityDto> cities) {
        return new FindCitiesResult("SUCCESS", null, cities);
    }

    public static FindCitiesResult failure(String errorMessage) {
        return new FindCitiesResult("FAILURE", errorMessage, null);
    }

    public static FindCitiesResult failure(String returnCode, String errorMessage) {
        return new FindCitiesResult(returnCode, errorMessage, null);
    }

    public List<CityDto> getCities() {
        return getData();
    }
}
