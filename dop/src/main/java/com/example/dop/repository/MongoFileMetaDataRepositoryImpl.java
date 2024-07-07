package com.example.dop.repository;

import com.example.dop.model.FileMetaData;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.util.List;

public class MongoFileMetaDataRepositoryImpl implements FileMetaDataRepository {
    private final MongoTemplate mongoTemplate;

    public MongoFileMetaDataRepositoryImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<FileMetaData> findAll() {
        return mongoTemplate.findAll(FileMetaData.class);
    }

    @Override
    public FileMetaData save(FileMetaData fileMetaData) {
        return mongoTemplate.save(fileMetaData);
    }

    @Override
    public void deleteAll() {
        mongoTemplate.dropCollection(FileMetaData.class);
    }


}
