package com.example.demo.usecase.ports.in.impl;

import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesOutput;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;

import java.util.List;

public class FindCitiesUseCaseImpl implements FindCitiesUseCase {
    @Override
    public FindCitiesOutput execute(FindCitiesInput input) {
        return new FindCitiesOutput(List.of());
    }
}
