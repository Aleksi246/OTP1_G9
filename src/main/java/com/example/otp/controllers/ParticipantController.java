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
import java.util.Map;

public class ParticipantController {
    private ParticipantDao participantDao = new ParticipantDao();
    private UserDao userDao = new UserDao();
    private CourseDao courseDao = new CourseDao();

    private void jsonMessage(Context ctx, HttpStatus status, String message) {
        ctx.status(status).json(Map.of("message", message));
    }

    public void enrollUser(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            int userId = body.get("userId").getAsInt();
            int classId = body.get("classId").getAsInt();

            Course course = courseDao.findById(classId);
            if (course == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Class does not exist");
                return;
            }

            User userToEnroll = userDao.findById(userId);
            if (userToEnroll == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "User to enroll does not exist");
                return;
            }

            boolean success = participantDao.addParticipant(userId, classId);
            if (success) {
                ctx.status(HttpStatus.CREATED).json(Map.of("message", "User enrolled", "userId", userId, "classId", classId));
            } else {
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to enroll user");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void unenrollUser(Context ctx) {
        try {
            String email = ctx.attribute("email");
            User unenrollingUser = userDao.findByEmail(email);
            if (unenrollingUser == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            int userId = body.get("userId").getAsInt();
            int classId = body.get("classId").getAsInt();

            Course course = courseDao.findById(classId);
            if (course == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Class does not exist");
                return;
            }
            boolean isSelfUnenroll = unenrollingUser.getUserId().equals(userId);
            boolean isClassCreator = unenrollingUser.getUserId().equals(course.getCreatorId());
            if (!isSelfUnenroll && !isClassCreator) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can unenroll others");
                return;
            }

            boolean success = participantDao.removeParticipant(userId, classId);
            if (success) {
                ctx.json(Map.of("message", "User unenrolled", "userId", userId, "classId", classId));
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Enrollment not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void getUsersByClass(Context ctx) {
        try {
            int classId = Integer.parseInt(ctx.pathParam("classId"));
            List<Integer> userIds = participantDao.findUsersByClass(classId);
            ctx.json(userIds);
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void getClassesByUser(Context ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            List<Integer> classIds = participantDao.findClassesByUser(userId);
            ctx.json(classIds);
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }
}