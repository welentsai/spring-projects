package com.example.demo.adapter.in.controller;

import com.example.demo.usecase.ports.in.FindCitiesInput;
import com.example.demo.usecase.ports.in.FindCitiesResult;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
public class CitiesController {
    private final FindCitiesUseCase findCityUseCase;

    public CitiesController(FindCitiesUseCase findCityUseCase) {
        this.findCityUseCase = findCityUseCase;
    }

    @GetMapping
    public ResponseEntity<FindCitiesResult> getAllCities() throws InterruptedException {
        Thread.sleep(1000);

        FindCitiesInput input = new FindCitiesInput();

        FindCitiesResult result = findCityUseCase.execute(input);

        if (result.isSuccess()) {
            return ResponseEntity.ok(result);
        } else {
            // Map different error codes to appropriate HTTP status codes
            HttpStatus status =
                    switch (result.getReturnCode()) {
                        case "NOT_FOUND" -> HttpStatus.NOT_FOUND;
                        case "VALIDATION_ERROR" -> HttpStatus.BAD_REQUEST;
                        case "INTERNAL_ERROR" -> HttpStatus.INTERNAL_SERVER_ERROR;
                        default -> HttpStatus.INTERNAL_SERVER_ERROR;
                    };
            return ResponseEntity.status(status).body(result);
        }
    }
}
