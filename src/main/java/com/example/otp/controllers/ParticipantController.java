package com.example.otp.controllers;

import com.example.otp.dao.ParticipantDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

public class ParticipantController {
    private ParticipantDao participantDao = new ParticipantDao();
    private UserDao userDao = new UserDao();

    public void enrollUser(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User enrollingUser = userDao.findByUsername(username);
            if (enrollingUser == null || !"teacher".equals(enrollingUser.getUserType())) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can enroll students");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            int userId = body.get("userId").getAsInt();
            int classId = body.get("classId").getAsInt();

            User userToEnroll = userDao.findById(userId);
            if (userToEnroll == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("User to enroll does not exist");
                return;
            }
            if ("admin".equals(userToEnroll.getUserType())) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Cannot enroll admin users");
                return;
            }

            boolean success = participantDao.addParticipant(userId, classId);
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("message", "User enrolled");
                response.addProperty("userId", userId);
                response.addProperty("classId", classId);
                ctx.status(HttpStatus.CREATED).json(response.toString());
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to enroll user");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void unenrollUser(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User unenrollingUser = userDao.findByUsername(username);
            if (unenrollingUser == null || !"teacher".equals(unenrollingUser.getUserType())) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can unenroll users");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            int userId = body.get("userId").getAsInt();
            int classId = body.get("classId").getAsInt();

            boolean success = participantDao.removeParticipant(userId, classId);
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("message", "User unenrolled");
                response.addProperty("userId", userId);
                response.addProperty("classId", classId);
                ctx.json(response.toString());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Enrollment not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getUsersByClass(Context ctx) {
        try {
            int classId = Integer.parseInt(ctx.pathParam("classId"));
            List<Integer> userIds = participantDao.findUsersByClass(classId);
            ctx.json(userIds);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getClassesByUser(Context ctx) {
        try {
            int userId = Integer.parseInt(ctx.pathParam("userId"));
            List<Integer> classIds = participantDao.findClassesByUser(userId);
            ctx.json(classIds);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }
}