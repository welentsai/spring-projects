package com.example.demo.adapter.in.dto;

import com.example.demo.usecase.ports.in.dto.CityDto;

import java.util.List;

public record FindCitiesResponse(List<CityDto> cities) {}
