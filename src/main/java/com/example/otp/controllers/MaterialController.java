package com.example.otp.controllers;

import com.example.otp.dao.CourseDao;
import com.example.otp.dao.MaterialDao;
import com.example.otp.dao.ParticipantDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.Course;
import com.example.otp.model.Material;
import com.example.otp.model.User;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import io.javalin.http.UploadedFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class MaterialController {
  private MaterialDao materialDao = new MaterialDao();
  private UserDao userDao = new UserDao();
  private CourseDao courseDao = new CourseDao();
  private ParticipantDao participantDao = new ParticipantDao();
  private static final Path UPLOAD_DIR = Paths.get("uploads").toAbsolutePath();

    private static final String ERROR = "Error: ";

    private void jsonMessage(Context ctx, HttpStatus status, String message) {
        ctx.status(status).json(Map.of("message", message));
    }

    private boolean isClassCreator(User user, Course course) {
        return user != null && course != null && user.getUserId() != null && user.getUserId().equals(course.getCreatorId());
    }

    public void getMaterialsByCourse(Context ctx) {
        try {
            int classId = Integer.parseInt(ctx.pathParam("classId"));
            List<Material> materials = materialDao.findByClassId(classId);
            ctx.json(materials);
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }

    public void getMaterialById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material != null) {
                ctx.json(material);
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Material not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }

    public void downloadMaterial(Context ctx) {
        try {
            String email = ctx.attribute("email");
            User user = userDao.findByEmail(email);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material == null) {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Material not found");
                return;
            }

            List<Integer> enrolledClassIds = participantDao.findClassesByUser(user.getUserId());
            if (!enrolledClassIds.contains(material.getClassId())) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "You must be enrolled in the course to download this material");
                return;
            }

            Path filePath = Paths.get(material.getFilepath());
            if (!Files.exists(filePath)) {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "File not found on server");
                return;
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            ctx.header("Content-Disposition", "attachment; filename=\"" + material.getOriginalFilename() + "\"");
            ctx.contentType(contentType);
            ctx.result(Files.newInputStream(filePath));
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }

    public void uploadMaterial(Context ctx) {
        try {
            String email = ctx.attribute("email");
            User user = userDao.findByEmail(email);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            UploadedFile file = ctx.uploadedFile("file");
            int classId = Integer.parseInt(ctx.formParam("classId"));
            String materialType = ctx.formParam("materialType");

            if (file == null || materialType == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Missing parameters");
                return;
            }

            Course course = courseDao.findById(classId);
            if (course == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Class does not exist");
                return;
            }
            if (!isClassCreator(user, course)) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can upload materials");
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
            material.setUserId(user.getUserId());

            Material created = materialDao.create(material);
            if (created.getFileId() != null) {
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to upload material");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }

    public void updateMaterial(Context ctx) {
        try {
            String email = ctx.attribute("email");
            User user = userDao.findByEmail(email);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material == null) {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Material not found");
                return;
            }

            Course course = courseDao.findById(material.getClassId());
            if (!isClassCreator(user, course)) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can update materials");
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
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update material");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }

    public void deleteMaterial(Context ctx) {
        try {
            String email = ctx.attribute("email");
            User user = userDao.findByEmail(email);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Material material = materialDao.findById(id);
            if (material != null) {
                Course course = courseDao.findById(material.getClassId());
                if (!isClassCreator(user, course)) {
                    jsonMessage(ctx, HttpStatus.FORBIDDEN, "Only class creator can delete materials");
                    return;
                }

                // Delete file
                Path filePath = Paths.get(material.getFilepath());
                Files.deleteIfExists(filePath);

                boolean success = materialDao.delete(id);
                if (success) {
                    ctx.json(Map.of("message", "Material deleted", "id", id));
                } else {
                    jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete material");
                }
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Material not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, ERROR + e.getMessage());
        }
    }
}