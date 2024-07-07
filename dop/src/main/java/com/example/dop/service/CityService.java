package com.example.dop.service;

import com.example.dop.model.CityEntity;

import java.util.List;

public interface CityService {
    List<CityEntity> getAllCities();
    CityEntity getCityById(String id);
    CityEntity addCity(CityEntity cityEntity);
}
