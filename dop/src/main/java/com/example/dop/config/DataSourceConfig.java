package com.example.dop.config;

import com.example.dop.repository.CityEntityRepository;
import com.example.dop.repository.FileMetaDataRepository;
import com.example.dop.repository.MongoCityEntityRepositoryImpl;
import com.example.dop.repository.MongoFileMetaDataRepositoryImpl;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.MongoTemplate;

@Configuration
public class DataSourceConfig {

    @Bean
    public CityEntityRepository getCityEntityRepository(MongoTemplate mongoTemplate) {
        return new MongoCityEntityRepositoryImpl(mongoTemplate);
    }

    @Bean
    public FileMetaDataRepository getFileMetaDataRepository(MongoTemplate mongoTemplate) {
        return new MongoFileMetaDataRepositoryImpl(mongoTemplate);
    }
}
