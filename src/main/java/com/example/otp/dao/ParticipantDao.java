// java
package com.example.otp.dao;

import com.example.otp.db.Database;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipantDao {

    public boolean addParticipant(int userId, int classId) throws SQLException {
        String sql = "INSERT INTO participants (user_id, class_id) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, classId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean removeParticipant(int userId, int classId) throws SQLException {
        String sql = "DELETE FROM participants WHERE user_id = ? AND class_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setInt(2, classId);
            return ps.executeUpdate() > 0;
        }
    }

    public List<Integer> findUsersByClass(int classId) throws SQLException {
        String sql = "SELECT user_id FROM participants WHERE class_id = ?";
        List<Integer> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("user_id"));
            }
        }
        return list;
    }

    public List<Integer> findClassesByUser(int userId) throws SQLException {
        String sql = "SELECT class_id FROM participants WHERE user_id = ?";
        List<Integer> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(rs.getInt("class_id"));
            }
        }
        return list;
    }
}