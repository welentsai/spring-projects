package com.example.dop.service;

import com.example.dop.model.CityEntity;
import com.example.dop.repository.CityRepository;

import java.util.List;

public class CityServiceImpl implements CityService {
    private final CityRepository cityRepository;

    public CityServiceImpl(CityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Override
    public List<CityEntity> getAllCities() {
        return cityRepository.findAll();
    }
}
