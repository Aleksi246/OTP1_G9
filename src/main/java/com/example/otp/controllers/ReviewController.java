 

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
import java.util.Objects;

public class ReviewController {
    private ReviewDao reviewDao = new ReviewDao();
    private UserDao userDao = new UserDao();

    private User getAuthenticatedUser(Context ctx) {
        String username = ctx.attribute("username");
        if (username == null) {
            String authHeader = ctx.header("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return null;
            }
            String token = authHeader.substring(7);
            username = JWTUtil.validateToken(token);
            if (username == null) {
                return null;
            }
        }
        try {
            return userDao.findByUsername(username);
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
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void getReviewById(Context ctx) {
        try {
            int id = Integer.parseInt(ctx.pathParam("id"));
            Review review = reviewDao.findById(id);
            if (review != null) {
                ctx.json(review);
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Review not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void createReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json("Authentication required");
                return;
            }

            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String reviewText = body.has("review") ? body.get("review").getAsString() : null;
            int rating = body.get("rating").getAsInt();
            int fileId = body.get("fileId").getAsInt();

            if (reviewText == null) {
                ctx.status(HttpStatus.BAD_REQUEST).json("Missing parameters");
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
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to create review");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void updateReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json("Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            JsonObject body = JsonParser.parseString(ctx.body()).getAsJsonObject();
            String reviewText = body.has("review") ? body.get("review").getAsString() : null;
            Integer rating = body.has("rating") ? body.get("rating").getAsInt() : null;

            Review review = reviewDao.findById(id);
            if (review == null) {
                ctx.status(HttpStatus.NOT_FOUND).json("Review not found");
                return;
            }

            if (!Objects.equals(user.getUserId(), review.getUserId())) {
                ctx.status(HttpStatus.FORBIDDEN).json("You can only update your own reviews");
                return;
            }

            if (reviewText != null) review.setReview(reviewText);
            if (rating != null) review.setRating(rating);

            boolean success = reviewDao.update(review);
            if (success) {
                ctx.json(review);
            } else {
                ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Failed to update review");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }

    public void deleteReview(Context ctx) {
        try {
            User user = getAuthenticatedUser(ctx);
            if (user == null) {
                ctx.status(HttpStatus.UNAUTHORIZED).json("Authentication required");
                return;
            }

            int id = Integer.parseInt(ctx.pathParam("id"));
            Review review = reviewDao.findById(id);
            if (review == null) {
                ctx.status(HttpStatus.NOT_FOUND).json("Review not found");
                return;
            }

            if (!Objects.equals(user.getUserId(), review.getUserId())) {
                ctx.status(HttpStatus.FORBIDDEN).json("You can only delete your own reviews");
                return;
            }

            boolean success = reviewDao.delete(id);
            if (success) {
                JsonObject response = new JsonObject();
                response.addProperty("message", "Review deleted");
                response.addProperty("id", id);
                ctx.json(response.toString());
            } else {
                ctx.status(HttpStatus.NOT_FOUND).json("Review not found");
            }
        } catch (Exception e) {
            ctx.status(HttpStatus.INTERNAL_SERVER_ERROR).json("Error: " + e.getMessage());
        }
    }
}