package com.example.demo.usecase.ports.in;

import com.example.demo.domain.model.City;

import java.util.List;

public record FindCitiesOutput(List<City> cities) implements Output {
}
