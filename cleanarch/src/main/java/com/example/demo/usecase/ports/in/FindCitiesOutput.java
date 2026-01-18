package com.example.demo.usecase.ports.in;

import com.example.demo.usecase.ports.in.dto.CityDto;

import java.util.List;

public class FindCitiesOutput extends Result<List<CityDto>> implements Output {

    private FindCitiesOutput(String returnCode, String errorMessage, List<CityDto> data) {
        super(returnCode, errorMessage, data);
    }

    public static FindCitiesOutput success(List<CityDto> cities) {
        return new FindCitiesOutput("SUCCESS", null, cities);
    }

    public static FindCitiesOutput failure(String errorMessage) {
        return new FindCitiesOutput("FAILURE", errorMessage, null);
    }

    public static FindCitiesOutput failure(String returnCode, String errorMessage) {
        return new FindCitiesOutput(returnCode, errorMessage, null);
    }

    public List<CityDto> getCities() {
        return getData();
    }
}
