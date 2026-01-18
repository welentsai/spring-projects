package com.example.demo.usecase.ports.out.repository;

import com.example.demo.usecase.ports.out.entity.CityJpaEntity;

import java.util.List;
import java.util.Optional;

public interface CitiesQueryRepository {
    /**
     * Find all cities using JDBC query
     *
     * @return List of all CityJpaEntity objects
     */
    List<CityJpaEntity> findAll();

    /**
     * Find city by name using JDBC query
     *
     * @param name the city name to search for
     * @return Optional containing the city if found
     */
    Optional<CityJpaEntity> findByName(String name);

    /**
     * Find all cities by country using JDBC query
     *
     * @param country the country to filter by
     * @return List of cities in the specified country
     */
    List<CityJpaEntity> findAllByCountry(String country);
}
