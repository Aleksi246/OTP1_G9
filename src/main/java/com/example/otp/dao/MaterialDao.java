// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.Material;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MaterialDao {

    public Material create(Material m) throws SQLException {
        String sql = "INSERT INTO materials (original_filename, stored_filename, filepath, material_type, class_id, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getOriginalFilename());
            ps.setString(2, m.getStoredFilename());
            ps.setString(3, m.getFilepath());
            ps.setString(4, m.getMaterialType());
            if (m.getClassId() != null) ps.setInt(5, m.getClassId()); else ps.setNull(5, Types.INTEGER);
            if (m.getUserId() != null) ps.setInt(6, m.getUserId()); else ps.setNull(6, Types.INTEGER);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) m.setFileId(rs.getInt(1));
            }
        }
        return m;
    }

    public Material findById(int id) throws SQLException {
        String sql = "SELECT file_id, original_filename, stored_filename, filepath, material_type, uploaded_at, class_id, user_id FROM materials WHERE file_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Material> findByClassId(int classId) throws SQLException {
        String sql = "SELECT file_id, original_filename, stored_filename, filepath, material_type, uploaded_at, class_id, user_id FROM materials WHERE class_id = ?";
        List<Material> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, classId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        }
        return list;
    }

    public boolean update(Material m) throws SQLException {
        if (m.getFileId() == null) return false;
        String sql = "UPDATE materials SET original_filename = ?, stored_filename = ?, filepath = ?, material_type = ?, class_id = ?, user_id = ? WHERE file_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, m.getOriginalFilename());
            ps.setString(2, m.getStoredFilename());
            ps.setString(3, m.getFilepath());
            ps.setString(4, m.getMaterialType());
            if (m.getClassId() != null) ps.setInt(5, m.getClassId()); else ps.setNull(5, Types.INTEGER);
            if (m.getUserId() != null) ps.setInt(6, m.getUserId()); else ps.setNull(6, Types.INTEGER);
            ps.setInt(7, m.getFileId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM materials WHERE file_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Material mapRow(ResultSet rs) throws SQLException {
        Material m = new Material();
        m.setFileId(rs.getInt("file_id"));
        m.setOriginalFilename(rs.getString("original_filename"));
        m.setStoredFilename(rs.getString("stored_filename"));
        m.setFilepath(rs.getString("filepath"));
        m.setMaterialType(rs.getString("material_type"));
        m.setUploadedAt(rs.getTimestamp("uploaded_at"));
        int cid = rs.getInt("class_id");
        if (!rs.wasNull()) m.setClassId(cid);
        int uid = rs.getInt("user_id");
        if (!rs.wasNull()) m.setUserId(uid);
        return m;
    }
}