// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.Review;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ReviewDao {

    private void validateRating(Integer rating) {
        if (rating == null) return;
        if (rating < 0 || rating > 5) throw new IllegalArgumentException("rating must be between 0 and 5");
    }

    public Review create(Review r) throws SQLException {
        validateRating(r.getRating());
        String sql = "INSERT INTO reviews (review, rating, file_id, user_id) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, r.getReview());
            if (r.getRating() != null) ps.setInt(2, r.getRating()); else ps.setNull(2, Types.INTEGER);
            if (r.getFileId() != null) ps.setInt(3, r.getFileId()); else ps.setNull(3, Types.INTEGER);
            if (r.getUserId() != null) ps.setInt(4, r.getUserId()); else ps.setNull(4, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) r.setReviewId(rs.getInt(1));
            }
        }
        return r;
    }

    public Review findById(int id) throws SQLException {
        String sql = "SELECT review_id, review, rating, file_id, user_id, created_at FROM reviews WHERE review_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Review> findByFileId(int fileId) throws SQLException {
        String sql = "SELECT review_id, review, rating, file_id, user_id, created_at FROM reviews WHERE file_id = ?";
        List<Review> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, fileId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(Review r) throws SQLException {
        if (r.getReviewId() == null) return false;
        validateRating(r.getRating());
        String sql = "UPDATE reviews SET review = ?, rating = ?, file_id = ?, user_id = ? WHERE review_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, r.getReview());
            if (r.getRating() != null) ps.setInt(2, r.getRating()); else ps.setNull(2, Types.INTEGER);
            if (r.getFileId() != null) ps.setInt(3, r.getFileId()); else ps.setNull(3, Types.INTEGER);
            if (r.getUserId() != null) ps.setInt(4, r.getUserId()); else ps.setNull(4, Types.INTEGER);
            ps.setInt(5, r.getReviewId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM reviews WHERE review_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Review mapRow(ResultSet rs) throws SQLException {
        Review r = new Review();
        r.setReviewId(rs.getInt("review_id"));
        r.setReview(rs.getString("review"));
        int rating = rs.getInt("rating");
        if (!rs.wasNull()) r.setRating(rating);
        int fid = rs.getInt("file_id");
        if (!rs.wasNull()) r.setFileId(fid);
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) r.setUserId(uid);
        r.setCreatedAt(rs.getTimestamp("created_at"));
        return r;
    }
}