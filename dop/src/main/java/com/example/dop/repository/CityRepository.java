package com.example.dop.repository;

import com.example.dop.model.CityEntity;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CityRepository extends MongoRepository<CityEntity, String> {
}
