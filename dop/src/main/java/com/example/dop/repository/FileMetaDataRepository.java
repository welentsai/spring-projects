package com.example.dop.repository;

import com.example.dop.model.FileMetaData;

import java.util.List;

public interface FileMetaDataRepository {
    List<FileMetaData> findAll();
    FileMetaData save(FileMetaData fileMetaData);
    void deleteAll();
}
