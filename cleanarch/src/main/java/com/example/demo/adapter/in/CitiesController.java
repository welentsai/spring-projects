package com.example.demo.adapter.in;

import com.example.demo.adapter.in.dto.FindCitiesRequest;
import com.example.demo.adapter.in.dto.FindCitiesResponse;
import com.example.demo.adapter.in.mapper.CitiesRequestMapper;
import com.example.demo.exception.BadRequestException;
import com.example.demo.usecase.ports.in.FindCitiesUseCase;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cities")
public class CitiesController {

    private final FindCitiesUseCase findCitiesUseCase;

    CitiesController(FindCitiesUseCase findCitiesUseCase) {
        this.findCitiesUseCase = findCitiesUseCase;
    }

    @GetMapping
    public ResponseEntity<FindCitiesResponse> getAllCities() throws InterruptedException {
        Thread.sleep(1000);

        FindCitiesResponse response = new FindCitiesRequest()
                .validate()
                .map(CitiesRequestMapper::toInput)
                .map(findCitiesUseCase::execute)
                .map(CitiesRequestMapper::toResponse)
                .orElseThrow(BadRequestException::new);

        return ResponseEntity.ok(response);
    }
}
