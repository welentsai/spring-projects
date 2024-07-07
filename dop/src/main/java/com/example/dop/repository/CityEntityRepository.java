package com.example.dop.repository;

import com.example.dop.model.CityEntity;

import java.util.List;

public interface CityEntityRepository {
    List<CityEntity> getAllCities();
    CityEntity getCityById(String id);
    CityEntity addCity(CityEntity cityEntity);
    void deleteAll();
}
