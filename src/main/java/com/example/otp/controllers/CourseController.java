package com.example.otp.controllers;

import com.example.otp.dao.CourseDao;
import com.example.otp.dao.ParticipantDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.Course;
import com.example.otp.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

public class CourseController {
    private CourseDao courseDao = new CourseDao();
    private UserDao userDao = new UserDao();
    private ParticipantDao participantDao = new ParticipantDao();

    private boolean isTeacher(User user) {
        return "teacher".equals(user.getUserType());
    }

    private boolean isEnrolled(User user, int classId) {
        try {
            List<Integer> classes = participantDao.findClassesByUser(user.getUserId());
            return classes.contains(classId);
        } catch (Exception e) {
            return false;
        }
    }

    public void getAllCourses(Context ctx) {
        try {
            List<Course> courses = courseDao.findAll();
            ctx.json(courses);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getCourseById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Course course = courseDao.findById(id);
            if (course != null) {
                ctx.json(course);
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Course not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void createCourse(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            if (user == null || !isTeacher(user)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can create courses");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String className = body.has("className") ? body.get("className").getAsString() : null;
            String topic = body.has("topic") ? body.get("topic").getAsString() : null;

            if (className == null || topic == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
                return;
            }

            Course course = new Course();
            course.setClassName(className);
            course.setTopic(topic);

            Course created = courseDao.create(course);
            if (created.getClassId() != null) {
                participantDao.addParticipant(user.getUserId(), created.getClassId());
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to create course");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void updateCourse(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (user == null || !isTeacher(user) || !isEnrolled(user, id)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only enrolled teachers can update courses");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String className = body.has("className") ? body.get("className").getAsString() : null;
            String topic = body.has("topic") ? body.get("topic").getAsString() : null;

            Course course = courseDao.findById(id);
            if (course == null) {
                ctx.status(HttpStatus.NOT_FOUND).json("Course not found");
                return;
            }

            if (className != null) course.setClassName(className);
            if (topic != null) course.setTopic(topic);

            boolean success = courseDao.update(course);
            if (success) {
                ctx.json(course);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to update course");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void deleteCourse(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            int id = Integer.parseInt(ctx.pathParam("id"));
            if (user == null || !isTeacher(user) || !isEnrolled(user, id)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only enrolled teachers can delete courses");
                return;
            }

            boolean success = courseDao.delete(id);
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("message", "Course deleted");
                response.addProperty("id", id);
                ctx.json(response.toString());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Course not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }
}