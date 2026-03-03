 

package com.example.otp.controllers;

import com.example.otp.dao.ReviewDao;
import com.example.otp.dao.UserDao;
import com.example.otp.model.User;
import com.example.otp.model.Review;
import com.example.otp.util.JWTUtil;
import io.javalin.http.Context;
import io.javalin.http.HttpStatus;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class ReviewController {
    private ReviewDao reviewDao = new ReviewDao();
    private UserDao userDao = new UserDao();

    private void jsonMessage(Context ctx, HttpStatus status, String message) {
        ctx.status(status).json(Map.of("message", message));
    }

    private User getAuthenticatedUser(Context ctx) {
        String email = ctx.attribute("email");
        if (email == null) {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.substring(7);
            email = JWTUtil.validateToken(token);
            if (email == null) {
                return null;
            }
        }
        try {
            return userDao.findByEmail(email);
        } catch (Exception e) {
            return null;
        }
    }

    public void getReviewsByMaterial(Context ctx) {
        try {
            int fileId = Integer.parseInt(ctx.pathParam("fileId"));
            List<Review> reviews = reviewDao.findByFileId(fileId);
            ctx.json(reviews);
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void getReviewById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Review review = reviewDao.findById(id);
            if (review != null) {
                ctx.json(review);
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Review not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void createReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String reviewText = body.has("review") ? body.get("review").getAsString() : null;
            int rating = body.get("rating").getAsInt();
            int fileId = body.get("fileId").getAsInt();

            if (reviewText == null) {
                jsonMessage(ctx, HttpStatus.BAD_REQUEST, "Missing parameters");
                return;
            }

            Review review = new Review();
            review.setReview(reviewText);
            review.setRating(rating);
            review.setFileId(fileId);
            review.setUserId(user.getUserId());

            Review created = reviewDao.create(review);
            if (created.getReviewId() != null) {
                ctx.status(HttpStatus.CREATED).json(created);
            } else {
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create review");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void updateReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String reviewText = body.has("review") ? body.get("review").getAsString() : null;
            Integer rating = body.has("rating") ? body.get("rating").getAsInt() : null;

            Review review = reviewDao.findById(id);
            if (review == null) {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Review not found");
                return;
            }

            if (!Objects.equals(user.getUserId(), review.getUserId())) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "You can only update your own reviews");
                return;
            }

            if (reviewText != null) review.setReview(reviewText);
            if (rating != null) review.setRating(rating);

            boolean success = reviewDao.update(review);
            if (success) {
                ctx.json(review);
            } else {
                jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update review");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }

    public void deleteReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                jsonMessage(ctx, HttpStatus.UNAUTHORIZED, "Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Review review = reviewDao.findById(id);
            if (review == null) {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Review not found");
                return;
            }

            if (!Objects.equals(user.getUserId(), review.getUserId())) {
                jsonMessage(ctx, HttpStatus.FORBIDDEN, "You can only delete your own reviews");
                return;
            }

            boolean success = reviewDao.delete(id);
            if (success) {
                ctx.json(Map.of("message", "Review deleted", "id", id));
            } else {
                jsonMessage(ctx, HttpStatus.NOT_FOUND, "Review not found");
            }
        } catch (Exception e) {
            jsonMessage(ctx, HttpStatus.INTERNAL_SERVER_ERROR, "Error: " + e.getMessage());
        }
    }
}