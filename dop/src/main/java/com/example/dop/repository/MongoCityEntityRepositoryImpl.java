package com.example.dop.repository;

import com.example.dop.model.CityEntity;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

public class MongoCityEntityRepositoryImpl implements CityEntityRepository {

    private final MongoTemplate mongoTemplate;

    public MongoCityEntityRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<CityEntity> getAllCities() {
        return mongoTemplate.findAll(CityEntity.class);
    }

    @Override
    public CityEntity getCityById(String id) {
        return mongoTemplate.findById(id, CityEntity.class);
    }

    @Override
    public CityEntity addCity(CityEntity cityEntity) {
        return mongoTemplate.save(cityEntity);
    }

    @Override
    public void deleteAll() {
        mongoTemplate.remove(new Query(), CityEntity.class);
    }
}
