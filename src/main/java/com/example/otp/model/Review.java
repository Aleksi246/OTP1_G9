// java
package com.example.otp.model;

import java.sql.Timestamp;

public class Review {
    private Integer reviewId;
    private String review;
    private Integer rating;
    private Integer fileId;
    private Integer userId;
    private Timestamp createdAt;

    public Review() {}

    public Review(Integer reviewId, String review, Integer rating, Integer fileId, Integer userId, Timestamp createdAt) {
        this.reviewId = reviewId;
        this.review = review;
        this.rating = rating;
        this.fileId = fileId;
        this.userId = userId;
        this.createdAt = createdAt;
    }

    public Integer getReviewId() { return reviewId; }
    public void setReviewId(Integer reviewId) { this.reviewId = reviewId; }
    public String getReview() { return review; }
    public void setReview(String review) { this.review = review; }
    public Integer getRating() { return rating; }
    public void setRating(Integer rating) { this.rating = rating; }
    public Integer getFileId() { return fileId; }
    public void setFileId(Integer fileId) { this.fileId = fileId; }
    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}