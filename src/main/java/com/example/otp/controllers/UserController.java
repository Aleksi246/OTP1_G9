package com.example.otp.controllers;

import com.example.otp.dao.UserDao;
import com.example.otp.model.User;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;

public class UserController {
    private UserDao userDao = new UserDao();

    public void register(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.has("username") ? body.get("username").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;
            String userType = body.has("userType") ? body.get("userType").getAsString() : null;

            if (username == null || password == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
                return;
            }

            User existing = userDao.findByUsername(username);
            if (existing != null) {
                ctx.status(HttpStatus.CONFLICT).json("Username already exists");
                return;
            }

            if ("admin".equals(userType)) {
                String adminKey = body.has("adminKey") ? body.get("adminKey").getAsString() : null;
                if (!"supersecretkey".equals(adminKey)) {
                    ctx.status(HttpStatus.FORBIDDEN).json("Invalid admin key");
                    return;
                }
            } else {
                userType = "student";
            }

            String hashedPassword = BCryptUtil.hashPassword(password);
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(hashedPassword);
            user.setUserType(userType);

            User created = userDao.create(user);
            if (created.getUserId() != null) {
                created.setPasswordHash(null);
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to register user");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void login(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String username = body.has("username") ? body.get("username").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;

            if (username == null || password == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
                return;
            }

            User user = userDao.findByUsername(username);
            if (user != null && BCryptUtil.verifyPassword(password, user.getPasswordHash())) {
                String token = JWTUtil.generateToken(username, user.getUserType());
                ctx.json("{\"token\":\"" + token + "\"}");
            } else {
                ctx.status(HttpStatus.UNAUTHORIZED).json("Invalid credentials");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getAllUsers(Context ctx) {
        try {
            List<User> users = userDao.findAll();
            for (User user : users) {
                user.setPasswordHash(null);
            }
            ctx.json(users);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getUserById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            User user = userDao.findById(id);
            if (user != null) {
                user.setPasswordHash(null);
                ctx.json(user);
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("User not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void createTeacher(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            if (user == null || !"admin".equals(user.getUserType())) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only admins can create teachers");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String teacherUsername = body.has("username") ? body.get("username").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;

            if (teacherUsername == null || password == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
                return;
            }

            String hashedPassword = BCryptUtil.hashPassword(password);
            User teacher = new User();
            teacher.setUsername(teacherUsername);
            teacher.setPasswordHash(hashedPassword);
            teacher.setUserType("teacher");

            User created = userDao.create(teacher);
            if (created.getUserId() != null) {
                created.setPasswordHash(null);
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to create teacher");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }
}