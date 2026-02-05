// java
package com.example.otp.dao;

import com.example.otp.db.Database;
import com.example.otp.model.Course;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CourseDao {

    public Course create(Course course) throws SQLException {
        String sql = "INSERT INTO classes (class_name, topic) VALUES (?, ?)";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, course.getClassName());
            ps.setString(2, course.getTopic());
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) course.setClassId(rs.getInt(1));
            }
        }
        return course;
    }

    public Course findById(int id) throws SQLException {
        String sql = "SELECT class_id, class_name, topic, created_at FROM classes WHERE class_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    public List<Course> findAll() throws SQLException {
        String sql = "SELECT class_id, class_name, topic, created_at FROM classes";
        List<Course> list = new ArrayList<>();
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public boolean update(Course course) throws SQLException {
        if (course.getClassId() == null) return false;
        String sql = "UPDATE classes SET class_name = ?, topic = ? WHERE class_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setString(1, course.getClassName());
            ps.setString(2, course.getTopic());
            ps.setInt(3, course.getClassId());
            return ps.executeUpdate() > 0;
        }
    }

    public boolean delete(int id) throws SQLException {
        String sql = "DELETE FROM classes WHERE class_id = ?";
        try (Connection c = Database.getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    private Course mapRow(ResultSet rs) throws SQLException {
        Course c = new Course();
        c.setClassId(rs.getInt("class_id"));
        c.setClassName(rs.getString("class_name"));
        c.setTopic(rs.getString("topic"));
        c.setCreatedAt(rs.getTimestamp("created_at"));
        return c;
    }
}