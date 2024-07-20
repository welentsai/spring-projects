package com.example.dop.controller;

import com.example.dop.domain.File;
import com.example.dop.model.FileMetaData;
import com.example.dop.repository.FileMetaDataRepository;
import com.example.dop.service.FileService;
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
    private final FileService fileService;
    private final FileMetaDataRepository fileMetaDataRepository;

    @Autowired
    public FileController(FileService fileService, FileMetaDataRepository fileMetaDataRepository) {
        this.fileService = fileService;
        this.fileMetaDataRepository = fileMetaDataRepository;
    }

    @GetMapping
    public ResponseEntity<List<FileMetaData>> listFiles() {
        var files = fileMetaDataRepository.findAll();
        fileMetaDataRepository.deleteAll();
        return ResponseEntity.ok(files);
    }

    @PostMapping("/upload")
    public ResponseEntity<String> uploadFile(@RequestParam("file") MultipartFile file, @RequestParam("type") String type) throws Exception {



//        File newFile = File.of(file);
        File newFile = fileService.uploadFile(file);

        return ResponseEntity.status(HttpStatus.CREATED).body(newFile.toString());
    }
}
