// java
package com.example.otp.model;

import java.sql.Timestamp;

public class Material {
    private Integer fileId;
    private String originalFilename;
    private String storedFilename;
    private String filepath;
    private String materialType;
    private Timestamp uploadedAt;
    private Integer classId;
    private Integer userId;

    public Material() {}

    public Material(Integer fileId, String originalFilename, String storedFilename, String filepath, String materialType, Timestamp uploadedAt, Integer classId, Integer userId) {
        this.fileId = fileId;
        this.originalFilename = originalFilename;
        this.storedFilename = storedFilename;
        this.filepath = filepath;
        this.materialType = materialType;
        this.uploadedAt = uploadedAt;
        this.classId = classId;
        this.userId = userId;
    }

    public Integer getFileId() { return fileId; }
    public void setFileId(Integer fileId) { this.fileId = fileId; }
    public String getOriginalFilename() { return originalFilename; }
    public void setOriginalFilename(String originalFilename) { this.originalFilename = originalFilename; }
    public String getStoredFilename() { return storedFilename; }
    public void setStoredFilename(String storedFilename) { this.storedFilename = storedFilename; }
    public String getFilepath() { return filepath; }
    public void setFilepath(String filepath) { this.filepath = filepath; }
    public String getMaterialType() { return materialType; }
    public void setMaterialType(String materialType) { this.materialType = materialType; }
    public Timestamp getUploadedAt() { return uploadedAt; }
    public void setUploadedAt(Timestamp uploadedAt) { this.uploadedAt = uploadedAt; }
    public Integer getClassId() { return classId; }
    public void setClassId(Integer classId) { this.classId = classId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
}