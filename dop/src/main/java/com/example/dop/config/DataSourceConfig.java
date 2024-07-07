package com.example.dop.config;

import com.example.dop.repository.CityEntityRepository;
import com.example.dop.repository.MongoCityEntityRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class DataSourceConfig {

    @Bean
    public CityEntityRepository getCityEntityRepository(MongoTemplate mongoTemplate) {
        return new MongoCityEntityRepositoryImpl(mongoTemplate);
    }
}
