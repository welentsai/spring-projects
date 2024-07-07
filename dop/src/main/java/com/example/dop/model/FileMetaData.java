package com.example.dop.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "files")
public class FileMetaData {

    @Id
    private String id;
    private String type;
    private String fileName;
    private String minioObjectName;

    public String getId() {return this.id;}
    public void setId(String id) {this.id = id;}

    public String getType() {return this.type;}
    public void setType(String type) {this.type = type;}

    public String getFileName() {return this.fileName;}
    public void setFileName(String fileName) {this.fileName = fileName;}

    public String getMinioObjectName() {return this.minioObjectName;}
    public void setMinioObjectName(String minioObjectName) {this.minioObjectName = minioObjectName;}

}
