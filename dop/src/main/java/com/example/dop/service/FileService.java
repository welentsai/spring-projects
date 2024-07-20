package com.example.dop.service;

import com.example.dop.domain.File;
import com.example.dop.repository.FileMetaDataRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileService {

    private final MinioService minioService;
    private final FileMetaDataRepository fileMetaDataRepository;

    @Autowired
    public FileService(MinioService minioService, FileMetaDataRepository fileMetaDataRepository) {
        this.minioService = minioService;
        this.fileMetaDataRepository = fileMetaDataRepository;
    }

    public File uploadFile(MultipartFile multipartFile) throws Exception {
        String objectName = minioService.uploadFile(multipartFile);

        //        FileMetaData metadata = new FileMetaData();
//        metadata.setType(type);
//        metadata.setFileName(file.getOriginalFilename());
//        metadata.setMinioObjectName(objectName);
//        var result = fileMetaDataRepository.save(metadata);

        File newFile = File.of(multipartFile, objectName);

        return newFile;
    }

}
