package com.example.otp.controllers;

import com.example.otp.dao.MaterialDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.Material;
import com.example.otp.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MaterialController {
    private MaterialDao materialDao = new MaterialDao();
    private UserDao userDao = new UserDao();
    private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath();

    private boolean isTeacher(User user) {
        return "teacher".equals(user.getUserType());
    }

    public void getMaterialsByCourse(Context ctx) {
        try {
            int classId = Integer.parseInt(ctx.pathParam("classId"));
            List<Material> materials = materialDao.findByClassId(classId);
            ctx.json(materials);
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getMaterialById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material != null) {
                ctx.json(material);
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Material not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void uploadMaterial(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            if (user == null || !isTeacher(user)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can upload materials");
                return;
            }

            UploadedFile file = ctx.uploadedFile("file");
            int classId = Integer.parseInt(ctx.formParam("classId"));
            int userId = Integer.parseInt(ctx.formParam("userId"));
            String materialType = ctx.formParam("materialType");

            if (file == null || materialType == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
                return;
            }

            // Ensure upload directory exists
            Path uploadPath = UPLOAD_DIR;
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            // Generate unique filename
            String originalFilename = file.filename();
            String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
            String storedFilename = UUID.randomUUID().toString() + extension;
            Path filePath = uploadPath.resolve(storedFilename);

            // Save file
            Files.copy(file.content(), filePath);

            Material material = new Material();
            material.setOriginalFilename(originalFilename);
            material.setStoredFilename(storedFilename);
            material.setFilepath(filePath.toString());
            material.setMaterialType(materialType);
            material.setClassId(classId);
            material.setUserId(userId);

            Material created = materialDao.create(material);
            if (created.getFileId() != null) {
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to upload material");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void updateMaterial(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            if (user == null || !isTeacher(user)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can update materials");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material == null) {
                ctx.status(HttpStatus.NOT_FOUND).json("Material not found");
                return;
            }

            // Try to parse JSON body
            String bodyStr = ctx.body();
            if (bodyStr != null && !bodyStr.isEmpty()) {
                try {
                    JsonObject body = JsonParser.parseString(bodyStr).getAsJsonObject();
                    String originalFilename = body.has("originalFilename") ? body.get("originalFilename").getAsString() : null;
                    String materialType = body.has("materialType") ? body.get("materialType").getAsString() : null;
                    if (originalFilename != null) material.setOriginalFilename(originalFilename);
                    if (materialType != null) material.setMaterialType(materialType);
                } catch (Exception e) {
                    // If JSON parsing fails, silently continue (body may be empty)
                }
            }

            boolean success = materialDao.update(material);
            if (success) {
                ctx.json(material);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to update material");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void deleteMaterial(Context ctx) {
        try {
            String username = ctx.attribute("username");
            User user = userDao.findByUsername(username);
            if (user == null || !isTeacher(user)) {
                ctx.status(HttpStatus.FORBIDDEN).json("Only teachers can delete materials");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material != null) {
                // Delete file
                Path filePath = Paths.get(material.getFilepath());
                Files.deleteIfExists(filePath);

                boolean success = materialDao.delete(id);
                if (success) {
                    JsonObject response = new JsonObject();
                    response.addProperty("message", "Material deleted");
                    response.addProperty("id", id);
                    ctx.json(response.toString());
                } else {
                    ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to delete material");
                }
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Material not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }
}