package com.example.dop.controller;

import com.example.dop.model.CityEntity;
import com.example.dop.service.CityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/cities")
public class CityController {
    private final CityService cityService;

    @Autowired
    public CityController(CityService cityService) {
        this.cityService = cityService;
    }

    @GetMapping
    public ResponseEntity<List<CityEntity>> getAllCities() throws InterruptedException {
        Thread.sleep(1000);
        List<CityEntity> cities = cityService.getAllCities();

        return ResponseEntity.ok(cities);
    }

    @GetMapping("/{id}")
    public ResponseEntity<CityEntity> getCityById(@PathVariable String id) {

        try {
            CityEntity city = cityService.getCityById(id);
            return ResponseEntity.ok(city);
        } catch (Exception e) {
//            return ResponseEntity.status(HttpStatus.NOT_FOUND).build();
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

    }

    @PostMapping
    public ResponseEntity<CityEntity> addCity(@RequestBody CityEntity addCityRequest) {
        CityEntity city = this.cityService.addCity(addCityRequest);

        URI newCityLocation = ServletUriComponentsBuilder
                .fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(city.getId())
                .toUri();

        return ResponseEntity.created(newCityLocation).body(city);
    }

}
