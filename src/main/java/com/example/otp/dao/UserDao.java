package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDao {

    public User create(User user) throws SQLException {
        String sql = "INSERT INTO users (username, password_hash, user_type, access_token) VALUES (?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getUserType());
            ps.setString(4, user.getAccessToken());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) user.setUserId(rs.getInt(1));
            }
        }
        return user;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, user_type, access_token, created_at FROM users WHERE user_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }
    
    public User findByUsername(String username) throws SQLException {
        String sql = "SELECT user_id, username, password_hash, user_type, access_token, created_at FROM users WHERE username = ?";
        try (Connection c = Database.getConnection();
            PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    public List<User> findAll() throws SQLException {
        String sql = "SELECT user_id, username, password_hash, user_type, access_token, created_at FROM users";
        List<User> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean update(User user) throws SQLException {
        if (user.getUserId() == null) return false;
        String sql = "UPDATE users SET username = ?, password_hash = ?, user_type = ?, access_token = ? WHERE user_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPasswordHash());
            ps.setString(3, user.getUserType());
            ps.setString(4, user.getAccessToken());
            ps.setInt(5, user.getUserId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM users WHERE user_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setUserId(rs.getInt("user_id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setUserType(rs.getString("user_type"));
        u.setAccessToken(rs.getString("access_token"));
        u.setCreatedAt(rs.getTimestamp("created_at"));
        return u;
    }
}