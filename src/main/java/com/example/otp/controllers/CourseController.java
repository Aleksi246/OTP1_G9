package com.example.otp.controllers;

import com.example.otp.dao.CourseDao;
import com.example.otp.dao.ParticipantDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.Course;
import com.example.otp.model.User;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;

public class CourseController {
  private CourseDao courseDao = new CourseDao();
  private UserDao userDao = new UserDao();
  private ParticipantDao participantDao = new ParticipantDao();

  private void jsonMessage(Context ctx, HttpStatus status, String message) {
    ctx.status(status).json(Map.of("message", message));
  }

  private boolean isClassCreator(User user, Course course) {
    return user != null && course != null && user
            .getUserId() != null && user
            .getUserId()
            .equals(course.getCreatorId());
  }

  public void getAllCourses(Context ctx) {
    try {
      List<Course> courses = courseDao.findAll();
      ctx.json(courses);
    } catch (Exception e) {
      jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
  }

  public void getCourseById(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      Course course = courseDao.findById(id);
      if (course != null) {
        ctx.json(course);
      } else {
        jsonMessage(ctx, HttpStatus.NOT_FOUND, "Course not found");
      }
    } catch (Exception e) {
      jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
  }

  public void createCourse(Context ctx) {
    try {
      String email = ctx.attribute("email");
      User user = userDao.findByEmail(email);
      if (user == null) {
        jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
        return;
      }

      JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
      String className = body.has("className") ? body.get("className").getAsString() : null;
      String topic = body.has("topic") ? body.get("topic").getAsString() : "";
      if (className == null || className.trim().isEmpty()) {
        jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Class name is required");
        return;
      }

      Course course = new Course();
      course.setClassName(className);
      course.setCreatorId(user.getUserId());
      course.setTopic(topic);

      Course created = courseDao.create(course);
      if (created.getClassId() != null) {
        participantDao.addParticipant(user.getUserId(), created.getClassId());
        ctx.status(HttpStatus.CREATED).json(created);
      } else {
        jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create course");
      }
    } catch (Exception e) {
      jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
  }

  public void updateCourse(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      String email = ctx.attribute("email");
      User user = userDao.findByEmail(email);
      if (user == null) {
        jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
        return;
      }

      Course course = courseDao.findById(id);
      if (course == null) {
        jsonMessage(ctx, HttpStatus.NOT_FOUND, "Course not found");
        return;
      }

      if (!isClassCreator(user, course)) {
        jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can update course");
        return;
      }

      JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
      String className = body.has("className") ? body.get("className").getAsString() : null;
      String topic = body.has("topic") ? body.get("topic").getAsString() : null;

      if (className != null) course.setClassName(className);
      if (topic != null) course.setTopic(topic);

      boolean success = courseDao.update(course);
      if (success) {
        ctx.json(course);
      } else {
        jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update course");
      }
    } catch (Exception e) {
      jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
  }

  public void deleteCourse(Context ctx) {
    try {
      int id = Integer.parseInt(ctx.pathParam("id"));
      String email = ctx.attribute("email");
      User user = userDao.findByEmail(email);
      if (user == null) {
        jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
        return;
      }

      Course course = courseDao.findById(id);
      if (course == null) {
        jsonMessage(ctx, HttpStatus.NOT_FOUND, "Course not found");
        return;
      }

      if (!isClassCreator(user, course)) {
        jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can delete course");
        return;
      }

      boolean success = courseDao.delete(id);
      if (success) {
        ctx.json(Map.of("message", "Course deleted", "id", id));
      } else {
        jsonMessage(ctx, HttpStatus.NOT_FOUND, "Course not found");
      }
    } catch (Exception e) {
      jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
    }
  }
}
