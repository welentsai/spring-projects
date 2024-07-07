package com.example.dop.service;

import com.example.dop.model.CityEntity;
import com.example.dop.repository.CityEntityRepository;

import java.util.List;

public class CityServiceImpl implements CityService {
//    private final CityRepository cityRepository;
    private final CityEntityRepository cityRepository;

    public CityServiceImpl(CityEntityRepository cityRepository) {
        this.cityRepository = cityRepository;
    }

    @Override
    public List<CityEntity> getAllCities() {
        return cityRepository.getAllCities();
    }

    @Override
    public CityEntity getCityById(String id) {
        return cityRepository.getCityById(id);
    }

    @Override
    public CityEntity addCity(CityEntity cityEntity) {
        return cityRepository.addCity(cityEntity);
    }
}
