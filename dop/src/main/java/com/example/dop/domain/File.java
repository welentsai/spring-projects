package com.example.dop.domain;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

public class File {
    private String id;
    private String originalFilename;
    private String objectName;
    private long size;
    private String contentType;
    private byte[] content;
    private List<String> tags;
    private int hitCount;

    private File() {
    }

    public static File of(@NotNull MultipartFile multipartFile, @NotNull String objectName) throws IOException {
        File file = new File();
        file.id = file.generateId(multipartFile);
        file.originalFilename = multipartFile.getOriginalFilename();
        file.objectName = objectName;
        file.size = multipartFile.getSize();
        file.contentType = multipartFile.getContentType();
        file.content = multipartFile.getBytes();
        file.tags = new ArrayList<>();
        file.hitCount = 0;
        return file;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOriginalFilename() {
        return originalFilename;
    }

    public void setOriginalFilename(String originalFilename) {
        this.originalFilename = originalFilename;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public void addTags(List<String> tags) {
        this.tags.addAll(tags);
    }

    public int getHitCount() {
        return hitCount;
    }

    public void setHitCount(int hitCount) {
        this.hitCount = hitCount;
    }

    private String generateId(MultipartFile file) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");

            // Combine multiple properties for uniqueness
            String uniqueString = String.format("%s_%d",
                    file.getOriginalFilename(),
                    file.getSize());

            byte[] hashBytes = md.digest(uniqueString.getBytes(StandardCharsets.UTF_8));

            // Convert byte array to hex string
            StringBuilder hexString = new StringBuilder();

            for (byte hashByte : hashBytes) {
                String hex = Integer.toHexString(0xff & hashByte);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }

            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to generate unique Id", e);
        }
    }

    @Override
    public String toString() {
        return "File{" +
                "id='" + id + '\'' +
                ", originalFilename='" + originalFilename + '\'' +
                ", minioObjectName='" + objectName + '\'' +
                ", size=" + size +
                ", contentType='" + contentType + '\'' +
                ", content(length)=" + content.length +
                ", tags=" + tags +
                ", hitCount=" + hitCount +
                '}';
    }
}
