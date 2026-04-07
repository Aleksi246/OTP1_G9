package com.example.otp.controllers;

import com.example.otp.dao.UserDao;
import com.example.otp.model.User;
import com.example.otp.util.BCryptUtil;
import com.example.otp.util.JWTUtil;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import java.util.List;
import java.util.Map;

public class UserController {
  private UserDao userDao = new UserDao();

  private void jsonMessage(Context ctx, HttpStatus status, String message) {
    ctx.status(status).json(Map.of("message", message));
  }

  public void register(Context ctx) {
    try {
      System.out.println("[REGISTRATION] Received registration request");
      JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
      String username = body.has("username") ? body.get("username").getAsString() : null;
      String password = body.has("password") ? body.get("password").getAsString() : null;
      String email = body.has("email") ? body.get("email").getAsString() : null;

      System.out.println("[REGISTRATION] Username: " + username + ", Email: " + email);

      if (username == null || password == null || email == null) {
        jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Missing parameters");
        return;
      }

      User existing = userDao.findByUsername(username);
      if (existing != null) {
        System.out.println("[REGISTRATION] User already exists: " + username);
        jsonMessage(ctx, HttpStatus.CONFLICT, "Username already exists");
        return;
      }

      User existingByEmail = userDao.findByEmail(email);
      if (existingByEmail != null) {
        System.out.println("[REGISTRATION] Email already exists: " + email);
        jsonMessage(ctx, HttpStatus.CONFLICT, "Email already exists");
        return;
      }

            String hashedPassword = BCryptUtil.hashPassword(password);
            User user = new User();
            user.setUsername(username);
            user.setPasswordHash(hashedPassword);
            user.setEmail(email);

            System.out.println("[REGISTRATION] Creating user in database: " + username);
            User created = userDao.create(user);
            if (created.getUserId() != null) {
                System.out.println("[REGISTRATION] User created successfully with ID: " + created.getUserId());
                created.setPasswordHash(null);
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                System.out.println("[REGISTRATION] Failed to register user - no ID returned");
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to register user");
            }
        } catch (Exception e) {
            System.err.println("[REGISTRATION] Exception: " + e.getMessage());
            e.printStackTrace();
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void login(Context ctx) {
        try {
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String email = body.has("email") ? body.get("email").getAsString() : null;
            String username = body.has("username") ? body.get("username").getAsString() : null;
            String password = body.has("password") ? body.get("password").getAsString() : null;

            if ((email == null || email.isBlank()) && (username == null || username.isBlank())
                    || password == null || password.isBlank()) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Missing parameters");
                return;
            }

            User user;
            if (email != null && !email.isBlank()) {
                user = userDao.findByEmail(email);
            } else {
                user = userDao.findByUsername(username);
                if (user == null && username != null && username.contains("@")) {
                    user = userDao.findByEmail(username);
                }
            }

            if (user != null && BCryptUtil.verifyPassword(password, user.getPasswordHash())) {
                String token = JWTUtil.generateToken(user.getEmail());
                ctx.json(Map.of(
                        "token", token,
                        "username", user.getUsername(),
                        "email", user.getEmail()
                ));
            } else {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
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
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
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
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "User not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void getUserByEmail(Context ctx) {
        try {
            String email = ctx.pathParam("email");
            User user = userDao.findByEmail(email);
            if (user != null) {
                user.setPasswordHash(null);
                ctx.json(user);
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "User not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void changePassword(Context ctx) {
        try {
            String email = ctx.attribute("email");
            if (email == null || email.isBlank()) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Unauthorized");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String currentPassword = body.has("currentPassword") ? body.get("currentPassword").getAsString() : null;
            String newPassword = body.has("newPassword") ? body.get("newPassword").getAsString() : null;

            if (currentPassword == null || currentPassword.isBlank() || newPassword == null || newPassword.isBlank()) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Missing parameters");
                return;
            }

            User user = userDao.findByEmail(email);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Unauthorized");
                return;
            }

            if (!BCryptUtil.verifyPassword(currentPassword, user.getPasswordHash())) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Current password is incorrect");
                return;
            }

            String newHash = BCryptUtil.hashPassword(newPassword);
            boolean updated = userDao.updatePassword(user.getUserId(), newHash);

            if (updated) {
                jsonMessage(ctx, HttpStatus.OK, "Password changed successfully");
            } else {
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to change password");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }
}