package com.example.app;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ClassController {

    @FXML
    private Label classNameLabel;

    @FXML
    private Label classIdLabel;

    @FXML
    private Label topicLabel;

    @FXML
    private Label creatorLabel;

    @FXML
    private Button joinButton;

    @FXML
    private MenuButton classActionsMenu;

    @FXML
    private MenuItem deleteClassMenuItem;

    @FXML
    private MenuItem leaveClassMenuItem;


    @FXML
    private VBox uploadSection;

    @FXML
    private Label selectedFileLabel;

    @FXML
    private ComboBox<String> materialTypeCombo;

    @FXML
    private Button uploadButton;

    @FXML
    private Label uploadProgressLabel;

    @FXML
    private ProgressBar uploadProgressBar;

    @FXML
    private Label materialsStatusLabel;

    @FXML
    private VBox materialsContainer;

    private Integer classId;
    private Integer userId;
    private Integer creatorId;
    private String token;

    private boolean isCreator;
    private boolean isEnrolled;

    private File selectedFile;
    private final HttpClient httpClient = HttpClient.newHttpClient();

    private static final int REVIEW_COMMENT_MAX_LENGTH = 500;

    @FXML
    private void initialize() {
        if (!SessionManager.isLoggedIn()) {
            Platform.runLater(SceneManager::loadLogin);
            return;
        }

        token = SessionManager.getToken();
        userId = SessionManager.getUserId();
        classId = getClassIdFromContext();

        materialTypeCombo.getItems().setAll("Lecture Notes", "Assignment", "Slides", "Reference", "Other");
        materialTypeCombo.getSelectionModel().selectFirst();

        setUploadSectionVisible(false);
        setUploadProgressVisible(false, "");

        if (classId == null) {
            showError("Error", "Class ID not provided");
            return;
        }

        loadClassDetails();
    }

    private Integer getClassIdFromContext() {
        Integer id = ClassContextHolder.getClassId();
        ClassContextHolder.clear();
        return id;
    }

    private void loadClassDetails() {
        runAsync(() -> {
            try {
                if (userId == null) {
                    userId = fetchUserId();
                    if (userId != null) {
                        SessionManager.setUserId(userId);
                    }
                }

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/courses/" + classId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Platform.runLater(() -> showError("Error", "Failed to load class details"));
                    return;
                }

                JsonObject courseJson = JsonParser.parseString(response.body()).getAsJsonObject();
                String className = getStringOrDefault(courseJson, "className", "Unknown class");
                String topic = getStringOrDefault(courseJson, "topic", "No topic");
                creatorId = courseJson.has("creatorId") && !courseJson.get("creatorId").isJsonNull()
                        ? courseJson.get("creatorId").getAsInt()
                        : null;

                isCreator = creatorId != null && creatorId.equals(userId);
                isEnrolled = isCreator || isUserEnrolled();

                Platform.runLater(() -> {
                    classNameLabel.setText(className);
                    classIdLabel.setText("Class ID: " + classId);
                    topicLabel.setText("Topic: " + topic);
                    creatorLabel.setText("Created by: User #" + (creatorId == null ? "Unknown" : creatorId));
                    updateAccessUi();
                });


                loadMaterials();
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error", "Failed to load class: " + e.getMessage()));
            }
        });
    }

    private Integer fetchUserId() {
        try {
            String email = SessionManager.getEmail();
            if (email == null || email.isBlank()) {
                return null;
            }

            String encodedEmail = URLEncoder.encode(email, StandardCharsets.UTF_8);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/users/by-email/" + encodedEmail))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return null;
            }

            JsonObject userJson = JsonParser.parseString(response.body()).getAsJsonObject();
            if (!userJson.has("userId") || userJson.get("userId").isJsonNull()) {
                return null;
            }
            return userJson.get("userId").getAsInt();
        } catch (Exception e) {
            return null;
        }
    }

    private boolean isUserEnrolled() {
        if (userId == null) {
            return false;
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/participants/user/" + userId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return false;
            }

            JsonArray classesArray = JsonParser.parseString(response.body()).getAsJsonArray();
            for (JsonElement element : classesArray) {
                if (element.getAsInt() == classId) {
                    return true;
                }
            }
            return false;
        } catch (Exception e) {
            return false;
        }
    }

    private void updateAccessUi() {
        if (isCreator) {
            joinButton.setText("Class Creator");
            joinButton.setDisable(true);
            deleteClassMenuItem.setVisible(true);
            leaveClassMenuItem.setVisible(false);
        } else if (isEnrolled) {
            joinButton.setText("Already Enrolled");
            joinButton.setDisable(true);
            deleteClassMenuItem.setVisible(false);
            leaveClassMenuItem.setVisible(true);
        } else {
            joinButton.setText("Join Class");
            joinButton.setDisable(false);
            deleteClassMenuItem.setVisible(false);
            leaveClassMenuItem.setVisible(false);
        }

        setUploadSectionVisible(isCreator);


        if (!isCreator && !isEnrolled) {
            materialsStatusLabel.setText("Join this class to access shared files.");
            materialsContainer.getChildren().clear();
        }
    }

    private void setUploadSectionVisible(boolean visible) {
        uploadSection.setVisible(visible);
        uploadSection.setManaged(visible);
    }


    private void loadMaterials() {
        if (!isCreator && !isEnrolled) {
            return;
        }

        runAsync(() -> {
            try {
                Platform.runLater(() -> {
                    materialsStatusLabel.setText("Loading files...");
                    materialsContainer.getChildren().clear();
                });

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/materials/course/" + classId))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() != 200) {
                    Platform.runLater(() -> materialsStatusLabel.setText("Could not load files right now."));
                    return;
                }

                JsonArray materials = JsonParser.parseString(response.body()).getAsJsonArray();
                List<JsonObject> rows = new ArrayList<>();
                for (JsonElement element : materials) {
                    if (element.isJsonObject()) {
                        rows.add(element.getAsJsonObject());
                    }
                }
                rows.sort(Comparator.comparing(o -> getStringOrDefault(o, "uploadedAt", ""), Comparator.reverseOrder()));

                Platform.runLater(() -> renderMaterials(rows));
            } catch (Exception e) {
                Platform.runLater(() -> materialsStatusLabel.setText("Could not load files."));
            }
        });
    }

    private void renderMaterials(List<JsonObject> materials) {
        materialsContainer.getChildren().clear();

        if (materials.isEmpty()) {
            materialsStatusLabel.setText("No files uploaded yet.");
            return;
        }

        materialsStatusLabel.setText(materials.size() + " file(s) available");

        for (JsonObject material : materials) {
            materialsContainer.getChildren().add(createMaterialCard(material));
        }
    }

    private HBox createMaterialCard(JsonObject material) {
        Integer fileId = material.has("fileId") && !material.get("fileId").isJsonNull() ? material.get("fileId").getAsInt() : null;
        String filename = getStringOrDefault(material, "originalFilename", "Unnamed file");
        String type = getStringOrDefault(material, "materialType", "Other");
        String uploadedAt = getStringOrDefault(material, "uploadedAt", "Unknown date");
        String uploader = material.has("userId") && !material.get("userId").isJsonNull()
                ? "Uploaded by user #" + material.get("userId").getAsInt()
                : "Uploader unknown";

        // File type badge color
        String badgeColor = switch (type.toLowerCase()) {
            case "lecture notes" -> "#3b82f6";
            case "assignment"    -> "#f59e0b";
            case "slides"        -> "#8b5cf6";
            case "reference"     -> "#06b6d4";
            default              -> "#64748b";
        };

        Label nameLabel = new Label(filename);
        nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI';");

        Label typeBadge = new Label(type);
        typeBadge.setStyle(
                "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white; -fx-font-family: 'Segoe UI';" +
                "-fx-background-color: " + badgeColor + "; -fx-background-radius: 20; -fx-padding: 2 9;"
        );

        Label metaLabel = new Label(uploader + "  ·  " + uploadedAt.replace("T", " "));
        metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI';");

        Label rowStatusLabel = new Label("");
        rowStatusLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #64748b; -fx-font-family: 'Segoe UI';");
        rowStatusLabel.setVisible(false);
        rowStatusLabel.setManaged(false);

        HBox badgeRow = new HBox(8, typeBadge);
        badgeRow.setAlignment(Pos.CENTER_LEFT);
        VBox infoBox = new VBox(5, nameLabel, badgeRow, metaLabel, rowStatusLabel);
        infoBox.setAlignment(Pos.CENTER_LEFT);

        String btnBase = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';" +
                         "-fx-padding: 7 14; -fx-background-radius: 7; -fx-cursor: hand;";

        Button downloadButton = new Button("⬇ Download");
        downloadButton.setStyle(btnBase + "-fx-background-color: #22c55e; -fx-text-fill: white;");
        downloadButton.setDisable(fileId == null);

        Button viewReviewsButton = new Button("📋 Reviews");
        viewReviewsButton.setStyle(btnBase + "-fx-background-color: #e2e8f0; -fx-text-fill: #334155;");
        viewReviewsButton.setDisable(fileId == null);

        Button reviewButton = new Button("⭐ Review");
        reviewButton.setStyle(btnBase + "-fx-background-color: #3b82f6; -fx-text-fill: white;");
        reviewButton.setDisable(fileId == null || !isEnrolled);
        reviewButton.setVisible(!isCreator);
        reviewButton.setManaged(!isCreator);

        Button deleteButton = new Button("🗑 Delete");
        deleteButton.setStyle(btnBase + "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
        deleteButton.setDisable(fileId == null);
        deleteButton.setVisible(isCreator);
        deleteButton.setManaged(isCreator);

        downloadButton.setOnAction(e -> handleDownloadMaterial(fileId, filename, downloadButton, deleteButton, rowStatusLabel));
        viewReviewsButton.setOnAction(e -> openViewReviewsDialog(fileId, filename));
        reviewButton.setOnAction(e -> openReviewDialog(fileId, filename, downloadButton, deleteButton, reviewButton, rowStatusLabel));
        deleteButton.setOnAction(e -> handleDeleteMaterial(fileId, filename, downloadButton, deleteButton, rowStatusLabel));

        HBox actions = new HBox(8, downloadButton, viewReviewsButton, reviewButton, deleteButton);
        actions.setAlignment(Pos.CENTER_RIGHT);
        HBox row = new HBox(16, infoBox, actions);
        row.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(infoBox, Priority.ALWAYS);
        row.setStyle(
                "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;" +
                "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14 16;"
        );
        return row;
    }

    private void openViewReviewsDialog(Integer fileId, String filename) {
        if (fileId == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/app/view-reviews-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            ViewReviewsDialogController controller = loader.getController();
            controller.setMaterialName(filename);
            controller.setStatus("Loading reviews...");

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Material Reviews");
            dialogStage.initOwner(classNameLabel.getScene().getWindow());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setResizable(false);
            dialogStage.setScene(scene);

            runAsync(() -> {
                ReviewsFetchResult result = fetchMaterialReviews(fileId);
                Platform.runLater(() -> controller.setReviewLines(result.reviewLines, result.message));
            });

            dialogStage.showAndWait();
        } catch (Exception e) {
            showError("Reviews", "Could not open reviews: " + e.getMessage());
        }
    }

    private ReviewsFetchResult fetchMaterialReviews(Integer fileId) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/reviews/material/" + fileId))
                    .header("Authorization", "Bearer " + token)
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                return new ReviewsFetchResult("Could not load reviews (" + response.statusCode() + ").", new ArrayList<>());
            }

            JsonArray reviewsArray = JsonParser.parseString(response.body()).getAsJsonArray();
            List<String> lines = new ArrayList<>();
            for (JsonElement element : reviewsArray) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject review = element.getAsJsonObject();
                String rating = review.has("rating") && !review.get("rating").isJsonNull()
                        ? String.valueOf(review.get("rating").getAsInt())
                        : "-";
                String reviewer = review.has("userId") && !review.get("userId").isJsonNull()
                        ? "User #" + review.get("userId").getAsInt()
                        : "Unknown user";
                String comment = getStringOrDefault(review, "comment", getStringOrDefault(review, "review", ""));
                if (comment.isBlank()) {
                    comment = "(No comment)";
                }
                lines.add(rating + "/5 - " + reviewer + " - " + comment);
            }

            String status = lines.isEmpty() ? "No reviews yet." : lines.size() + " review(s)";
            return new ReviewsFetchResult(status, lines);
        } catch (Exception e) {
            return new ReviewsFetchResult("Review list API unavailable. Connect backend GET /api/reviews/material/{fileId}.", new ArrayList<>());
        }
    }

    private void openReviewDialog(Integer fileId,
                                  String filename,
                                  Button downloadButton,
                                  Button deleteButton,
                                  Button reviewButton,
                                  Label rowStatusLabel) {
        if (isCreator) {
            showError("Forbidden", "Creators cannot review class materials.");
            return;
        }
        if (fileId == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/example/app/review-dialog.fxml"));
            Scene scene = new Scene(loader.load());

            ReviewDialogController dialogController = loader.getController();
            dialogController.setMaterialName(filename);

            Stage dialogStage = new Stage();
            dialogStage.setTitle("Review Material");
            dialogStage.initOwner(classNameLabel.getScene().getWindow());
            dialogStage.initModality(Modality.WINDOW_MODAL);
            dialogStage.setResizable(false);
            dialogStage.setScene(scene);
            dialogStage.showAndWait();

            if (dialogController.isSubmitted()) {
                handleSubmitReview(
                        fileId,
                        dialogController.getSelectedRating(),
                        dialogController.getComment(),
                        downloadButton,
                        deleteButton,
                        reviewButton,
                        rowStatusLabel
                );
            }
        } catch (Exception e) {
            showError("Review", "Could not open review form: " + e.getMessage());
        }
    }

    private void handleSubmitReview(Integer fileId,
                                    Integer rating,
                                    String comment,
                                    Button downloadButton,
                                    Button deleteButton,
                                    Button reviewButton,
                                    Label rowStatusLabel) {
        if (isCreator) {
            showError("Forbidden", "Creators cannot review class materials.");
            return;
        }

        String trimmedComment = comment == null ? "" : comment.trim();
        if (rating == null || rating < 1 || rating > 5) {
            showError("Invalid review", "Rating must be between 1 and 5.");
            return;
        }
        if (trimmedComment.isBlank() || trimmedComment.length() > REVIEW_COMMENT_MAX_LENGTH) {
            showError("Invalid review", "Comment must be between 1 and " + REVIEW_COMMENT_MAX_LENGTH + " characters.");
            return;
        }

        downloadButton.setDisable(true);
        reviewButton.setDisable(true);
        if (deleteButton != null && deleteButton.isManaged()) {
            deleteButton.setDisable(true);
        }
        rowStatusLabel.setText("Submitting review...");
        rowStatusLabel.setVisible(true);
        rowStatusLabel.setManaged(true);

        runAsync(() -> {
            ReviewSubmitResult result = submitMaterialReview(fileId, rating, trimmedComment);
            Platform.runLater(() -> {
                downloadButton.setDisable(false);
                reviewButton.setDisable(false);
                if (deleteButton != null && deleteButton.isManaged()) {
                    deleteButton.setDisable(false);
                }

                rowStatusLabel.setText(result.message);
                rowStatusLabel.setVisible(true);
                rowStatusLabel.setManaged(true);

                if (result.success) {
                    showSuccess("Review submitted", "Thanks! Your review was submitted.");
                } else {
                    showError("Review failed", result.message);
                }
            });
        });
    }

    private ReviewSubmitResult submitMaterialReview(Integer fileId, Integer rating, String comment) {
        try {
            JsonObject reviewData = new JsonObject();
            reviewData.addProperty("fileId", fileId);
            reviewData.addProperty("rating", rating);
            reviewData.addProperty("review", comment);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(new URI("http://localhost:7700/api/reviews"))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(reviewData.toString()))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 || response.statusCode() == 201) {
                return new ReviewSubmitResult(true, "Review submitted");
            }

            String fallbackMessage = "Could not submit review right now (" + response.statusCode() + ").";
            return new ReviewSubmitResult(false, extractApiMessage(response.body(), fallbackMessage));
        } catch (Exception e) {
            return new ReviewSubmitResult(false, "Review API unavailable. Connect backend endpoint /api/reviews. Details: " + e.getMessage());
        }
    }

    @FXML
    private void handleJoinClass() {
        runAsync(() -> {
            try {
                JsonObject enrollData = new JsonObject();
                enrollData.addProperty("userId", userId);
                enrollData.addProperty("classId", classId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/participants/enroll"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(enrollData.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        isEnrolled = true;
                        updateAccessUi();
                        loadMaterials();
                        showSuccess("Success", "You have successfully joined the class!");
                    } else {
                        showError("Error", "Failed to join class: " + response.statusCode());
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error", "Failed to join class: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleChooseFile() {
        if (!isCreator) {
            showError("Forbidden", "Only the class creator can upload materials.");
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Material File");
        File file = chooser.showOpenDialog(classNameLabel.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            selectedFileLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleUploadMaterial() {
        if (!isCreator) {
            showError("Forbidden", "Only the class creator can upload materials.");
            return;
        }
        if (selectedFile == null) {
            showError("Missing file", "Choose a file before uploading.");
            return;
        }

        String materialType = materialTypeCombo.getValue();
        if (materialType == null || materialType.isBlank()) {
            showError("Missing type", "Select a material type.");
            return;
        }

        File fileToUpload = selectedFile;
        setUploadProgressVisible(true, "Uploading " + fileToUpload.getName() + "...");
        uploadButton.setDisable(true);
        runAsync(() -> {
            try {
                String boundary = "----OTPBoundary" + System.currentTimeMillis();
                byte[] body = buildMultipartBody(fileToUpload, boundary, classId, materialType);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/materials"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    uploadButton.setDisable(false);
                    setUploadProgressVisible(false, "");
                    if (response.statusCode() == 201 || response.statusCode() == 200) {
                        selectedFile = null;
                        selectedFileLabel.setText("No file selected");
                        showSuccess("Uploaded", "Material uploaded successfully.");
                        loadMaterials();
                    } else {
                        showError("Upload failed", extractApiMessage(response.body(), "Server responded with " + response.statusCode()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    uploadButton.setDisable(false);
                    setUploadProgressVisible(false, "");
                    showError("Upload failed", e.getMessage());
                });
            }
        });
    }

    private byte[] buildMultipartBody(File file, String boundary, Integer classId, String materialType) throws IOException {
        String crlf = "\r\n";
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        writeTextPart(output, boundary, "classId", String.valueOf(classId));
        writeTextPart(output, boundary, "materialType", materialType);

        String contentType = Files.probeContentType(file.toPath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        output.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"" + file.getName() + "\"" + crlf)
                .getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Type: " + contentType + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        output.write(Files.readAllBytes(file.toPath()));
        output.write(crlf.getBytes(StandardCharsets.UTF_8));

        output.write(("--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));
        return output.toByteArray();
    }

    private void writeTextPart(ByteArrayOutputStream output, String boundary, String name, String value) throws IOException {
        String crlf = "\r\n";
        output.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
        output.write(("Content-Disposition: form-data; name=\"" + name + "\"" + crlf + crlf).getBytes(StandardCharsets.UTF_8));
        output.write(value.getBytes(StandardCharsets.UTF_8));
        output.write(crlf.getBytes(StandardCharsets.UTF_8));
    }

    private void handleDownloadMaterial(Integer fileId, String filename, Button downloadButton, Button deleteButton, Label rowStatusLabel) {
        if (fileId == null) {
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Material");
        chooser.setInitialFileName(filename == null || filename.isBlank() ? "material" : filename);

        File destination = chooser.showSaveDialog(classNameLabel.getScene().getWindow());
        if (destination == null) {
            return;
        }

        setRowActionState(downloadButton, deleteButton, rowStatusLabel, true, "Downloading...");
        runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/materials/" + fileId + "/download"))
                        .header("Authorization", "Bearer " + token)
                        .GET()
                        .build();

                HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());

                if (response.statusCode() == 200) {
                    Files.write(destination.toPath(), response.body());
                    Platform.runLater(() -> {
                        setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
                        showSuccess("Download complete", "Saved to: " + destination.getAbsolutePath());
                    });
                } else {
                    Platform.runLater(() -> {
                        setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
                        showError("Download failed", "Server responded with " + response.statusCode());
                    });
                }
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
                    showError("Download failed", e.getMessage());
                });
            }
        });
    }

    private void handleDeleteMaterial(Integer fileId, String filename, Button downloadButton, Button deleteButton, Label rowStatusLabel) {
        if (!isCreator || fileId == null) {
            return;
        }

        setRowActionState(downloadButton, deleteButton, rowStatusLabel, true, "Deleting...");
        runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/materials/" + fileId))
                        .header("Authorization", "Bearer " + token)
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                Platform.runLater(() -> {
                    setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
                    if (response.statusCode() == 200) {
                        showSuccess("Deleted", "Removed: " + filename);
                        loadMaterials();
                    } else {
                        showError("Delete failed", extractApiMessage(response.body(), "Server responded with " + response.statusCode()));
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
                    showError("Delete failed", e.getMessage());
                });
            }
        });
    }

    @FXML
    private void handleRefreshMaterials() {
        loadMaterials();
    }

    @FXML
    private void handleGoBack() {
        SceneManager.goBack();
    }

    private String getStringOrDefault(JsonObject obj, String field, String fallback) {
        return obj.has(field) && !obj.get(field).isJsonNull() ? obj.get(field).getAsString() : fallback;
    }

    private void runAsync(Runnable task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void setUploadProgressVisible(boolean visible, String message) {
        uploadProgressLabel.setText(message == null ? "" : message);
        uploadProgressLabel.setVisible(visible);
        uploadProgressLabel.setManaged(visible);
        uploadProgressBar.setVisible(visible);
        uploadProgressBar.setManaged(visible);
        uploadProgressBar.setProgress(visible ? ProgressBar.INDETERMINATE_PROGRESS : 0);
    }

    private void setRowActionState(Button downloadButton, Button deleteButton, Label rowStatusLabel, boolean busy, String message) {
        downloadButton.setDisable(busy);
        if (deleteButton != null && deleteButton.isManaged()) {
            deleteButton.setDisable(busy);
        }

        rowStatusLabel.setText(message == null ? "" : message);
        rowStatusLabel.setVisible(busy);
        rowStatusLabel.setManaged(busy);
    }

    private String extractApiMessage(String responseBody, String fallback) {
        try {
            JsonElement parsed = JsonParser.parseString(responseBody);
            if (parsed.isJsonObject()) {
                JsonObject obj = parsed.getAsJsonObject();
                if (obj.has("message") && !obj.get("message").isJsonNull()) {
                    return obj.get("message").getAsString();
                }
            }
        } catch (Exception ignored) {
        }
        return fallback;
    }

    @FXML
    private void handleDeleteClass() {
        if (!isCreator) {
            showError("Error", "Only the class creator can delete this class.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Delete Class");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("Deleting this class will remove it permanently for all users. This action cannot be undone.");

        if (confirmDialog.showAndWait().isEmpty() || confirmDialog.getResult() != ButtonType.OK) {
            return;
        }

        runAsync(() -> {
            try {
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/courses/" + classId))
                        .header("Authorization", "Bearer " + token)
                        .DELETE()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 204) {
                    Platform.runLater(() -> {
                        showSuccess("Success", "Class deleted successfully.");
                        SceneManager.loadHome();
                    });
                } else {
                    Platform.runLater(() -> showError("Error", "Failed to delete class. Status: " + response.statusCode()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error", "Failed to delete class: " + e.getMessage()));
            }
        });
    }

    @FXML
    private void handleLeaveClass() {
        if (!isEnrolled || isCreator) {
            showError("Error", "You are not enrolled in this class or you are the creator.");
            return;
        }

        Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
        confirmDialog.setTitle("Leave Class");
        confirmDialog.setHeaderText("Are you sure?");
        confirmDialog.setContentText("You will be removed from this class and lose access to the materials.");

        if (confirmDialog.showAndWait().isEmpty() || confirmDialog.getResult() != ButtonType.OK) {
            return;
        }

        runAsync(() -> {
            try {
                JsonObject unenrollData = new JsonObject();
                unenrollData.addProperty("userId", userId);
                unenrollData.addProperty("classId", classId);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(new URI("http://localhost:7700/api/participants/unenroll"))
                        .header("Authorization", "Bearer " + token)
                        .header("Content-Type", "application/json")
                        .method("DELETE", HttpRequest.BodyPublishers.ofString(unenrollData.toString()))
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 || response.statusCode() == 204) {
                    Platform.runLater(() -> {
                        showSuccess("Success", "You have left the class.");
                        SceneManager.loadHome();
                    });
                } else {
                    Platform.runLater(() -> showError("Error", "Failed to leave class. Status: " + response.statusCode()));
                }
            } catch (Exception e) {
                Platform.runLater(() -> showError("Error", "Failed to leave class: " + e.getMessage()));
            }
        });
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showSuccess(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private static class ReviewSubmitResult {
        private final boolean success;
        private final String message;

        private ReviewSubmitResult(boolean success, String message) {
            this.success = success;
            this.message = message;
        }
    }

    private static class ReviewsFetchResult {
        private final String message;
        private final List<String> reviewLines;

        private ReviewsFetchResult(String message, List<String> reviewLines) {
            this.message = message;
            this.reviewLines = reviewLines;
        }
    }
}

class ClassContextHolder {
    private static Integer classId;

    public static void setClassId(Integer id) {
        classId = id;
    }

    public static Integer getClassId() {
        return classId;
    }

    public static void clear() {
        classId = null;
    }
}
