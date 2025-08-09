package com.example.demo.adapter.in.controller;

import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesOutput;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/cities")
public class CitiesController {
    private final FindCitiesUseCase findCityUseCase;

    public CitiesController(FindCitiesUseCase findCityUseCase) {
        this.findCityUseCase = findCityUseCase;
    }

    @GetMapping
    public ResponseEntity<FindCitiesOutput> getAllCities() throws InterruptedException {
        Thread.sleep(1000);

        FindCitiesInput input = new FindCitiesInput();

        FindCitiesOutput cities = findCityUseCase.execute(input);

        return ResponseEntity.ok(cities);
    }

}
