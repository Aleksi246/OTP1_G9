package com.example.otp.model;

import java.sql.Timestamp;

public class User {
    private Integer userId;
    private String username;
    private String passwordHash;
    private String userType;
    private String accessToken;
    private Timestamp createdAt;

    public User() {}

    public User(Integer userId, String username, String passwordHash, String userType, String accessToken, Timestamp createdAt) {
        this.userId = userId;
        this.username = username;
        this.passwordHash = passwordHash;
        this.userType = userType;
        this.accessToken = accessToken;
        this.createdAt = createdAt;
    }

    // getters and setters
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getUserType() { return userType; }
    public void setUserType(String userType) { this.userType = userType; }
    public String getAccessToken() { return accessToken; }
    public void setAccessToken(String accessToken) { this.accessToken = accessToken; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}