package com.example.dop.controller;

import com.example.dop.model.FileMetaData;
import com.example.dop.repository.FileMetaDataRepository;
import com.example.dop.service.MinioService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("api/v1/files")
public class FileController {
    private final MinioService minioService;
    private final FileMetaDataRepository fileMetaDataRepository;

    @Autowired
    public FileController(MinioService minioService, FileMetaDataRepository fileMetaDataRepository) {
        this.minioService = minioService;
        this.fileMetaDataRepository = fileMetaDataRepository;
    }

    @GetMapping
    public ResponseEntity<List<FileMetaData>> listFiles() {
        var files = fileMetaDataRepository.findAll();
        fileMetaDataRepository.deleteAll();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<FileMetaData> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("type") String type) throws Exception {
        String objectName = minioService.uploadFile(file);

        FileMetaData metadata = new FileMetaData();
        metadata.setType(type);
        metadata.setFileName(file.getOriginalFilename());
        metadata.setMinioObjectName(objectName);
        var result = fileMetaDataRepository.save(metadata);

        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }
}
