package com.example.app;

import com.google.gson.*;
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
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
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

public class ClassController {

  @FXML private Label classNameLabel;
  @FXML private Label classIdLabel;
  @FXML private Label topicLabel;
  @FXML private Label creatorLabel;
  @FXML private Button joinButton;
  @FXML private MenuButton classActionsMenu;
  @FXML private MenuItem deleteClassMenuItem;
  @FXML private MenuItem leaveClassMenuItem;
  @FXML private VBox uploadSection;
  @FXML private Label selectedFileLabel;
  @FXML private ComboBox<String> materialTypeCombo;
  @FXML private Button uploadButton;
  @FXML private Label uploadProgressLabel;
  @FXML private ProgressBar uploadProgressBar;
  @FXML private Label materialsStatusLabel;
  @FXML private VBox materialsContainer;

  private Integer classId;
  private Integer userId;
  private Integer creatorId;
  private String token;
  private boolean isCreator;
  private boolean isEnrolled;
  private File selectedFile;
  private final HttpClient httpClient = HttpClient.newHttpClient();
  private final String apiBaseUrl = System.getProperty("otp.api.base-url", "http://localhost:7700");
  private static final int REVIEW_COMMENT_MAX_LENGTH = 500;
  private static final String MATERIAL_LECTURE_NOTES = "Lecture Notes";
  private static final String MATERIAL_ASSIGNMENT = "Assignment";
  private static final String MATERIAL_SLIDES = "Slides";
  private static final String MATERIAL_REFERENCE = "Reference";
  private static final String MATERIAL_OTHER = "Other";

  // Error constants
  private static final String ERROR_UPLOAD_FAILED = "class.error.uploadFailed";
  private static final String ERROR_TITLE = "class.error.title";
  private static final String ERROR_REVIEW_TITLE = "class.error.review.title";
  private static final String ERROR_JOINFAILED = "class.error.joinFailed";
  private static final String ERROR_SERVER_RESPONSE = "class.error.serverResponse";

  // Success constants
  private static final String SUCCESS_TITLE = "home.success.title";

  // Literals
  private static final String USER_ID = "userId";
  private static final String AUTHORIZATION = "Authorization";
  private static final String BEARER = "Bearer ";

  private URI apiUri(String path) {
    return URI.create(apiBaseUrl + path);
  }

  @FXML
  private void initialize() {
    if (!SessionManager.isLoggedIn()) {
      Platform.runLater(SceneManager::loadLogin);
      return;
    }

    token = SessionManager.getToken();
    userId = SessionManager.getUserId();
    classId = ClassContextHolder.getClassId();

    materialTypeCombo.getItems().setAll(
        LocaleManager.getString("class.materialType.lectureNotes"),
        LocaleManager.getString("class.materialType.assignment"),
        LocaleManager.getString("class.materialType.slides"),
        LocaleManager.getString("class.materialType.reference"),
        LocaleManager.getString("class.materialType.other")
    );
    materialTypeCombo.getSelectionModel().selectFirst();

    setUploadSectionVisible(false);
    setUploadProgressVisible(false, "");

    if (classId == null) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getString(ERROR_TITLE),
          LocaleManager.getString("class.error.missingId"));
      return;
    }

    loadClassDetails();
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
            .uri(apiUri("/api/courses/" + classId))
                .header(AUTHORIZATION, BEARER + token)
                .GET()
                .build();

        HttpResponse<String> response = httpClient.send(request,
                HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
          Platform.runLater(() -> showAlert(Alert.AlertType.ERROR,
                  LocaleManager.getString(ERROR_TITLE),
                  LocaleManager.getString("class.error.loadDetails")));
          return;
        }

        JsonObject courseJson = JsonParser.parseString(response.body()).getAsJsonObject();
        String className = getStringOrDefault(courseJson, "className",
                LocaleManager.getString("class.unknown"));
        String topic = getStringOrDefault(courseJson, "topic",
                LocaleManager.getString("class.noTopic"));
        creatorId = courseJson.has("creatorId") && !courseJson.get("creatorId").isJsonNull()
                        ? courseJson.get("creatorId").getAsInt()
                        : null;

        isCreator = creatorId != null && creatorId.equals(userId);
        isEnrolled = isCreator || isUserEnrolled();

        Platform.runLater(() -> {
          classNameLabel.setText(className);
          classIdLabel.setText(LocaleManager.getString("class.classId", classId));
          topicLabel.setText(LocaleManager.getString("class.topic", topic));
          creatorLabel.setText(LocaleManager.getString("class.creator", 
                  creatorId == null ? LocaleManager.getString("class.creatorUnknown") : creatorId));
          updateAccessUi();
        });


        loadMaterials();
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Platform.runLater(() -> showAlert(
                Alert.AlertType.ERROR,
                LocaleManager.getString(ERROR_TITLE),
                LocaleManager.getString("class.error.failedLoad", e.getMessage())));
      } catch (Exception e) {
        Platform.runLater(() -> showAlert(
            Alert.AlertType.ERROR,
            LocaleManager.getString(ERROR_TITLE),
            LocaleManager.getString("class.error.failedLoad", e.getMessage())));
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
          .uri(apiUri("/api/users/by-email/" + encodedEmail))
                  .header(AUTHORIZATION, BEARER + token)
                  .GET()
                  .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers
              .ofString());
      if (response.statusCode() != 200) {
        return null;
      }

      JsonObject userJson = JsonParser.parseString(response.body()).getAsJsonObject();
      if (!userJson.has(USER_ID) || userJson.get(USER_ID).isJsonNull()) {
        return null;
      }
      return userJson.get(USER_ID).getAsInt();
    } catch (Exception e) {
      Thread.currentThread().interrupt();
      return null;
    }
  }

  private boolean isUserEnrolled() {
    if (userId == null) {
      return false;
    }

    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(apiUri("/api/participants/user/" + userId))
                  .header(AUTHORIZATION, BEARER + token)
                  .GET()
                  .build();

      HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers
              .ofString());
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
      Thread.currentThread().interrupt();
      return false;
    }
  }

  private void updateAccessUi() {
    var bundle = LocaleManager.getBundle();
    if (isCreator) {
      joinButton.setText(bundle.getString("class.classCreator"));
      joinButton.setDisable(true);
      deleteClassMenuItem.setVisible(true);
      leaveClassMenuItem.setVisible(false);
    } else if (isEnrolled) {
      joinButton.setText(bundle.getString("class.alreadyEnrolled"));
      joinButton.setDisable(true);
      deleteClassMenuItem.setVisible(false);
      leaveClassMenuItem.setVisible(true);
    } else {
      joinButton.setText(bundle.getString("class.joinClass"));
      joinButton.setDisable(false);
      deleteClassMenuItem.setVisible(false);
      leaveClassMenuItem.setVisible(false);
    }

    setUploadSectionVisible(isCreator);

    if (!isCreator && !isEnrolled) {
      materialsStatusLabel.setText(bundle.getString("class.joinToAccess"));
      materialsContainer.getChildren().clear();
    }
  }

  private void setUploadSectionVisible(boolean visible) {
    uploadSection.setVisible(visible);
    uploadSection.setManaged(visible);
  }

  private void loadMaterials() {
    if (!isCreator && !isEnrolled) return;

    runAsync(() -> {
      try {
        Platform.runLater(() -> {
          materialsStatusLabel.setText(LocaleManager.getString("class.loadingFiles"));
          materialsContainer.getChildren().clear();
        });

        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/materials/course/" + classId))
            .header(AUTHORIZATION, BEARER + token)
            .GET()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers
                .ofString());

        if (response.statusCode() != 200) {
          Platform.runLater(() -> materialsStatusLabel.setText(LocaleManager.getString(
                  "class.couldNotLoadFiles")));
          return;
        }

        JsonArray materials = JsonParser.parseString(response.body()).getAsJsonArray();
        List<JsonObject> rows = new ArrayList<>();
        for (JsonElement element : materials) {
          if (element.isJsonObject()) {
            rows.add(element.getAsJsonObject());
          }
        }
        rows.sort(Comparator.comparing(o -> getStringOrDefault(o, "uploadedAt", ""),
                Comparator.reverseOrder()));

        Platform.runLater(() -> renderMaterials(rows));
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        Platform.runLater(() -> materialsStatusLabel.setText(LocaleManager.getString(
                "class.couldNotLoadFilesShort")));
      } catch (Exception e) {
        Platform.runLater(() -> materialsStatusLabel.setText(LocaleManager.getString(
                "class.couldNotLoadFilesShort")));
      }
    });
  }

  private void renderMaterials(List<JsonObject> materials) {
    materialsContainer.getChildren().clear();

    if (materials.isEmpty()) {
      materialsStatusLabel.setText(LocaleManager.getString("class.noFilesUploadedYet"));
      return;
    }

    materialsStatusLabel.setText(LocaleManager.getString("class.filesAvailable", materials.size()));

    for (JsonObject material : materials) {
      materialsContainer.getChildren().add(createMaterialCard(material));
    }
  }

  private HBox createMaterialCard(JsonObject material) {
    Integer fileId = material.has("fileId") && !material.get("fileId").isJsonNull() ? material.get(
            "fileId").getAsInt() : null;
    String filename = getStringOrDefault(material, "originalFilename",
        LocaleManager.getString("class.unnamedFile"));
    String type = getStringOrDefault(material, "materialType", MATERIAL_OTHER);
    String localizedType = localizeMaterialType(type);
    String uploadedAt = getStringOrDefault(material, "uploadedAt",
        LocaleManager.getString("class.unknownDate"));
    String uploader = material.has(USER_ID) && !material.get(USER_ID).isJsonNull()
            ? LocaleManager.getString("class.uploadedByUser", material.get(USER_ID).getAsInt())
            : LocaleManager.getString("class.uploaderUnknown");

    // File type badge color
    String badgeColor = switch (normalizeMaterialType(type)) {
      case "lecture notes" -> "#3b82f6";
      case "assignment"    -> "#f59e0b";
      case "slides"        -> "#8b5cf6";
      case "reference"     -> "#06b6d4";
      default              -> "#64748b";
    };

    Label nameLabel = new Label(filename);
    nameLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;"
            + "-fx-text-fill: #0f172a; -fx-font-family: 'Segoe UI';");

    Label typeBadge = new Label(localizedType);
    typeBadge.setStyle(
        "-fx-font-size: 11px; -fx-font-weight: bold; -fx-text-fill: white;"
                + " -fx-font-family: 'Segoe UI'; -fx-background-color: " + badgeColor
                + "; -fx-background-radius: 20; -fx-padding: 2 9;"
    );

    Label metaLabel = new Label(uploader + "  ·  " + uploadedAt.replace("T", " "));
    metaLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #94a3b8; -fx-font-family: 'Segoe UI';");

    Label rowStatusLabel = new Label("");
    rowStatusLabel.setStyle("-fx-font-size: 11px;"
            + "-fx-text-fill: #64748b; -fx-font-family: 'Segoe UI';");
    rowStatusLabel.setVisible(false);
    rowStatusLabel.setManaged(false);

    HBox badgeRow = new HBox(8, typeBadge);
    badgeRow.setAlignment(Pos.CENTER_LEFT);
    VBox infoBox = new VBox(5, nameLabel, badgeRow, metaLabel, rowStatusLabel);
    infoBox.setAlignment(Pos.CENTER_LEFT);

    String btnBase = "-fx-font-size: 12px; -fx-font-weight: bold; -fx-font-family: 'Segoe UI';"
            + "-fx-padding: 7 14; -fx-background-radius: 7; -fx-cursor: hand;";

    Button downloadButton = new Button(LocaleManager.getString("class.download"));
    downloadButton.setStyle(btnBase + "-fx-background-color: #22c55e; -fx-text-fill: white;");
    downloadButton.setDisable(fileId == null);

    Button viewReviewsButton = new Button(LocaleManager.getString("class.reviews"));
    viewReviewsButton.setStyle(btnBase + "-fx-background-color: #e2e8f0; -fx-text-fill: #334155;");
    viewReviewsButton.setDisable(fileId == null);

    Button reviewButton = new Button(LocaleManager.getString("class.review"));
    reviewButton.setStyle(btnBase + "-fx-background-color: #3b82f6; -fx-text-fill: white;");
    reviewButton.setDisable(fileId == null || !isEnrolled);
    reviewButton.setVisible(!isCreator);
    reviewButton.setManaged(!isCreator);

    Button deleteButton = new Button(LocaleManager.getString("class.delete"));
    deleteButton.setStyle(btnBase + "-fx-background-color: #fee2e2; -fx-text-fill: #dc2626;");
    deleteButton.setDisable(fileId == null);
    deleteButton.setVisible(isCreator);
    deleteButton.setManaged(isCreator);

    downloadButton
            .setOnAction(e
                    -> handleDownloadMaterial(fileId,
                    filename, downloadButton, deleteButton, rowStatusLabel));
    viewReviewsButton
            .setOnAction(e
                    -> openViewReviewsDialog(fileId, filename));
    reviewButton
            .setOnAction(e
                    -> openReviewDialog(fileId,
                    filename, downloadButton, deleteButton, reviewButton, rowStatusLabel));
    deleteButton
            .setOnAction(e
                    -> handleDeleteMaterial(fileId,
                    filename, downloadButton, deleteButton, rowStatusLabel));

    HBox actions = new HBox(8, downloadButton, viewReviewsButton, reviewButton, deleteButton);
    actions.setAlignment(Pos.CENTER_RIGHT);
    HBox row = new HBox(16, infoBox, actions);
    row.setAlignment(Pos.CENTER_LEFT);
    HBox.setHgrow(infoBox, Priority.ALWAYS);
    row.setStyle(
            "-fx-background-color: #f8fafc; -fx-border-color: #e2e8f0;"
                    + "-fx-border-radius: 10; -fx-background-radius: 10; -fx-padding: 14 16;"
    );
    return row;
  }

  private void openViewReviewsDialog(Integer fileId, String filename) {
    if (fileId == null) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(getClass()
              .getResource("/com/example/app/view-reviews-dialog.fxml"));
      loader.setResources(LocaleManager.getBundle());
      Scene scene = new Scene(loader.load());

      ViewReviewsDialogController controller = loader.getController();
      controller.setMaterialName(filename);
      controller.setStatus(LocaleManager.getBundle().getString("viewreviews.loading"));

      Stage dialogStage = new Stage();
      dialogStage.setTitle(LocaleManager.getBundle().getString("viewreviews.title"));
      dialogStage.initOwner(classNameLabel.getScene().getWindow());
      dialogStage.initModality(Modality.WINDOW_MODAL);
      dialogStage.setResizable(false);
      dialogStage.setScene(scene);

      runAsync(() -> {
        ReviewsFetchResult result = fetchMaterialReviews(fileId);
        Platform.runLater(() -> controller.setReviewLines(result.reviewLines(), result.message()));
      });

      dialogStage.showAndWait();
    } catch (Exception e) {
      showAlert(Alert.AlertType.ERROR, LocaleManager
                      .getBundle().getString("class.error.reviews.title"),
          MessageFormat.format(LocaleManager
                .getBundle().getString("class.error.openReviews"), e.getMessage()));
    }
  }

  private ReviewsFetchResult fetchMaterialReviews(Integer fileId) {
    try {
      HttpRequest request = HttpRequest.newBuilder()
          .uri(apiUri("/api/reviews/material/" + fileId))
          .header(AUTHORIZATION, BEARER + token)
          .GET()
          .build();

      HttpResponse<String> response = httpClient
              .send(request, HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() != 200) {
        return new ReviewsFetchResult(
            LocaleManager.getString("class.error.reviewsWithCode", response.statusCode()),
            new ArrayList<>()
        );
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
        String reviewer = review.has(USER_ID) && !review.get(USER_ID).isJsonNull()
                        ? MessageFormat.format(LocaleManager.getBundle()
                        .getString("class.reviewer"),
                        review.get(USER_ID).getAsInt())
                        : LocaleManager.getBundle().getString("class.unknownUser");
        String comment = getStringOrDefault(review,
                    "comment",
                    getStringOrDefault(review,
                    "review",
                    ""));
        if (comment.isBlank()) {
          comment = LocaleManager.getBundle().getString("class.noComment");
        }
        lines
            .add(MessageFormat
            .format(LocaleManager
            .getBundle()
            .getString("class.reviewLine"),
            rating,
            reviewer,
            comment));
      }

      String status = lines.isEmpty()
                    ? LocaleManager.getBundle().getString("class.noReviewsYet")
                    : MessageFormat
                      .format(LocaleManager
                      .getBundle()
                      .getString("class.reviewCount"),
                      lines
                      .size());
      return new ReviewsFetchResult(status, lines);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ReviewsFetchResult(LocaleManager
          .getBundle()
          .getString("class.error.reviewListUnavailable"),
          new ArrayList<>());
    } catch (Exception e) {
      return new ReviewsFetchResult(LocaleManager
              .getBundle()
              .getString("class.error.reviewListUnavailable"),
              new ArrayList<>());
    }
  }

  private void openReviewDialog(Integer fileId,
                                String filename,
                                Button downloadButton,
                                Button deleteButton,
                                Button reviewButton,
                                Label rowStatusLabel) {
    if (isCreator) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
          LocaleManager.getBundle().getString("class.error.forbiddenReview"));
      return;
    }
    if (fileId == null) {
      return;
    }

    try {
      FXMLLoader loader = new FXMLLoader(getClass()
          .getResource("/com/example/app/review-dialog.fxml"));
      loader
          .setResources(LocaleManager
          .getBundle());
      Scene scene = new Scene(loader.load());

      ReviewDialogController dialogController = loader.getController();
      dialogController.setMaterialName(filename);

      Stage dialogStage = new Stage();
      dialogStage.setTitle(LocaleManager.getBundle().getString("review.dialog.title"));
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
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_REVIEW_TITLE),
                    MessageFormat
                            .format(LocaleManager
                                    .getBundle()
                                    .getString("class.error.openReviewForm"),
                                    e
                                            .getMessage()));
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
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
          LocaleManager.getBundle().getString("class.error.forbiddenReview"));
      return;
    }

    String trimmedComment = comment == null ? "" : comment.trim();
    if (rating == null || rating < 1 || rating > 5) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle()
          .getString(ERROR_REVIEW_TITLE),
          LocaleManager
          .getBundle()
          .getString("class.error.invalidRating"));
      return;
    }
    if (trimmedComment.isBlank() || trimmedComment.length() > REVIEW_COMMENT_MAX_LENGTH) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_REVIEW_TITLE),
          MessageFormat
              .format(LocaleManager
              .getBundle()
              .getString("class.error.invalidComment"),
              REVIEW_COMMENT_MAX_LENGTH));
      return;
    }

    downloadButton.setDisable(true);
    reviewButton.setDisable(true);
    if (deleteButton != null && deleteButton.isManaged()) {
      deleteButton.setDisable(true);
    }
    rowStatusLabel.setText(LocaleManager.getBundle().getString("class.status.submittingReview"));
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

        rowStatusLabel.setText(result.message());
        rowStatusLabel.setVisible(true);
        rowStatusLabel.setManaged(true);

        if (result.success()) {
          showAlert(Alert.AlertType.INFORMATION, LocaleManager.getBundle().getString(SUCCESS_TITLE),
              LocaleManager.getBundle().getString("class.success.reviewSubmitted"));
        } else {
          showAlert(Alert
              .AlertType
              .ERROR,
              LocaleManager
              .getBundle()
              .getString("class.error.reviewFailed"),
              result
              .message());
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
            .uri(apiUri("/api/reviews"))
            .header(AUTHORIZATION, BEARER + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(reviewData.toString()))
            .build();

      HttpResponse<String> response = httpClient.send(request,
          HttpResponse.BodyHandlers.ofString());
      if (response.statusCode() == 200 || response.statusCode() == 201) {
        return new ReviewSubmitResult(true,
            LocaleManager
            .getString("class.success.reviewSubmitted"));
      }

      String fallbackMessage = LocaleManager.getString("class.error.reviewSubmitWithCode",
          response.statusCode());
      return new ReviewSubmitResult(false, extractApiMessage(response.body(), fallbackMessage));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new ReviewSubmitResult(false,
          LocaleManager.getString("class.error.reviewApiUnavailable", e.getMessage()));
    } catch (Exception e) {
      return new ReviewSubmitResult(false,
          LocaleManager
          .getString("class.error.reviewApiUnavailable",
          e
          .getMessage()));
    }
  }

  @FXML
  private void handleJoinClass() {
    runAsync(() -> {
      try {
        JsonObject enrollData = new JsonObject();
        enrollData.addProperty(USER_ID, userId);
        enrollData.addProperty("classId", classId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/participants/enroll"))
            .header(AUTHORIZATION, BEARER + token)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(enrollData.toString()))
            .build();

        HttpResponse<String> response = httpClient
            .send(request,
            HttpResponse
            .BodyHandlers
            .ofString());
        Platform.runLater(() -> {
          if (response.statusCode() == 201 || response.statusCode() == 200) {
            isEnrolled = true;
            updateAccessUi();
            loadMaterials();
            showAlert(Alert
                .AlertType
                .INFORMATION,
                LocaleManager
                .getBundle()
                .getString(SUCCESS_TITLE),
                LocaleManager
                .getBundle()
                .getString("class.success.joined"));
          } else {
            showAlert(Alert
                .AlertType
                .ERROR,
                LocaleManager
                .getBundle()
                .getString(ERROR_TITLE),
                MessageFormat
                .format(LocaleManager
                .getBundle()
                .getString(ERROR_JOINFAILED),
                response
                .statusCode()));
          }
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
            MessageFormat.format(LocaleManager
            .getBundle()
            .getString(ERROR_JOINFAILED), e.getMessage()));
      } catch (Exception e) {
        Platform.runLater(() -> showAlert(Alert
                        .AlertType
                        .ERROR,
                LocaleManager
                        .getBundle()
                        .getString(ERROR_TITLE),
            MessageFormat
            .format(LocaleManager
            .getBundle()
            .getString(ERROR_JOINFAILED),
            e
            .getMessage())));
      }
    });
  }

  @FXML
  private void handleChooseFile() {
    if (!isCreator) {
      showAlert(Alert
          .AlertType
          .ERROR,
          LocaleManager
          .getBundle()
          .getString(ERROR_TITLE),
          LocaleManager
          .getBundle()
          .getString("class.error.onlyCreatorUpload"));
      return;
    }

    FileChooser chooser = new FileChooser();
    chooser.setTitle(LocaleManager.getBundle().getString("class.dialog.selectMaterialFile"));
    File file = chooser.showOpenDialog(classNameLabel.getScene().getWindow());
    if (file != null) {
      selectedFile = file;
      selectedFileLabel.setText(file.getName());
    }
  }

  @FXML
  private void handleUploadMaterial() {
    if (!isCreator) {
      showAlert(Alert
          .AlertType
          .ERROR,
          LocaleManager
          .getBundle()
          .getString(ERROR_TITLE),
          LocaleManager
          .getBundle()
          .getString("class.error.onlyCreatorUpload"));
      return;
    }
    if (selectedFile == null) {
      showAlert(Alert
          .AlertType
          .ERROR,
          LocaleManager
          .getBundle()
          .getString(ERROR_TITLE),
          LocaleManager.getBundle().getString("class.error.missingFile"));
      return;
    }

    String selectedMaterialType = materialTypeCombo.getValue();
    if (selectedMaterialType == null || selectedMaterialType.isBlank()) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
                    LocaleManager.getBundle().getString("class.error.missingType"));
      return;
    }
    String materialType = toApiMaterialType(selectedMaterialType);

    File fileToUpload = selectedFile;
    setUploadProgressVisible(true,
        MessageFormat
        .format(LocaleManager
        .getBundle()
        .getString("class.status.uploadingFile"),
        fileToUpload
        .getName()));
    uploadButton.setDisable(true);
    runAsync(() -> {
      try {
        String boundary = "----OTPBoundary" + System.currentTimeMillis();
        byte[] body = buildMultipartBody(fileToUpload, boundary, classId, materialType);

        HttpRequest request = HttpRequest.newBuilder()
                    .uri(apiUri("/api/materials"))
                        .header(AUTHORIZATION, BEARER + token)
                        .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                        .POST(HttpRequest.BodyPublishers.ofByteArray(body))
                        .build();

        HttpResponse<String> response = httpClient
                    .send(request,
                    HttpResponse
                    .BodyHandlers
                    .ofString());

        Platform.runLater(() -> {
          uploadButton.setDisable(false);
          setUploadProgressVisible(false, "");
          if (response.statusCode() == 201 || response.statusCode() == 200) {
            selectedFile = null;
            selectedFileLabel
                .setText(LocaleManager
                .getBundle()
                .getString("class.noFileSelected"));
            showAlert(Alert
                .AlertType
                .INFORMATION,
                LocaleManager
                .getBundle()
                .getString(SUCCESS_TITLE),
                LocaleManager
                .getBundle()
                .getString("class.success.uploaded"));
            loadMaterials();
          } else {
            showAlert(Alert
                .AlertType
                .ERROR,
                LocaleManager
                .getBundle()
                .getString(ERROR_UPLOAD_FAILED),
                extractApiMessage(response
                .body(),
                MessageFormat
                .format(LocaleManager
                .getBundle()
                .getString(ERROR_SERVER_RESPONSE),
                response
                .statusCode())));
          }
        });
      } catch (InterruptedException e) {
        Thread
        .currentThread().interrupt();
        Platform.runLater(() -> {
          uploadButton
              .setDisable(false);
          setUploadProgressVisible(false, "");
          showAlert(Alert
              .AlertType
              .ERROR,
              LocaleManager
              .getBundle()
              .getString(ERROR_UPLOAD_FAILED),
              e
              .getMessage());
        });
      } catch (Exception e) {
        Platform.runLater(() -> {
          uploadButton.setDisable(false);
          setUploadProgressVisible(false, "");
          showAlert(Alert
                      .AlertType
                      .ERROR,
                      LocaleManager
                      .getBundle()
                      .getString(ERROR_UPLOAD_FAILED),
                      e
                      .getMessage());
        });
      }
    });
  }

  private byte[] buildMultipartBody(File file,
                                    String boundary,
                                    Integer classId,
                                    String materialType) throws IOException {
    String crlf = "\r\n";
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    writeTextPart(output, boundary, "classId", String.valueOf(classId));
    writeTextPart(output, boundary, "materialType", materialType);
    String contentType = Files.probeContentType(file.toPath());
    if (contentType == null) {
      contentType = "application/octet-stream";
    }
    output
        .write(("--"
        + boundary
        + crlf)
        .getBytes(StandardCharsets
        .UTF_8));
    output
        .write(("Content-Disposition: form-data; name=\"file\"; filename=\""
        + file
        .getName()
        + "\""
        + crlf)
        .getBytes(StandardCharsets.UTF_8));
    output
        .write(("Content-Type: "
        + contentType
        + crlf
        + crlf)
        .getBytes(StandardCharsets
        .UTF_8));
    output.write(Files.readAllBytes(file.toPath()));
    output.write(crlf.getBytes(StandardCharsets.UTF_8));
    output.write(("--" + boundary + "--" + crlf).getBytes(StandardCharsets.UTF_8));
    return output.toByteArray();
  }

  private void writeTextPart(ByteArrayOutputStream output,
                             String boundary,
                             String name,
                             String value) throws IOException {
    String crlf = "\r\n";
    output.write(("--" + boundary + crlf).getBytes(StandardCharsets.UTF_8));
    output
        .write(("Content-Disposition: form-data; name=\""
        + name
        + "\""
        + crlf
        + crlf)
        .getBytes(StandardCharsets
        .UTF_8));
    output.write(value.getBytes(StandardCharsets.UTF_8));
    output.write(crlf.getBytes(StandardCharsets.UTF_8));
  }

  private void handleDownloadMaterial(Integer fileId,
                                      String filename,
                                      Button downloadButton,
                                      Button deleteButton,
                                      Label rowStatusLabel) {
    if (fileId == null) {
      return;
    }

    FileChooser chooser = new FileChooser();
    chooser.setTitle(LocaleManager.getBundle().getString("class.dialog.saveMaterial"));
    chooser
            .setInitialFileName(filename == null || filename
                    .isBlank() ? LocaleManager
                                 .getBundle()
                                 .getString("class.unknown") : filename);

    File destination = chooser.showSaveDialog(classNameLabel.getScene().getWindow());
    if (destination == null) {
      return;
    }

    setRowActionState(downloadButton, deleteButton, rowStatusLabel, true,
        LocaleManager
        .getBundle()
        .getString("class.status.downloading"));
    runAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/materials/" + fileId + "/download"))
            .header(AUTHORIZATION, BEARER + token)
            .GET()
            .build();

        HttpResponse<byte[]> response = httpClient
            .send(request,
            HttpResponse
            .BodyHandlers
            .ofByteArray());

        if (response.statusCode() == 200) {
          Files.write(destination.toPath(), response.body());
          Platform.runLater(() -> {
            setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
            showAlert(Alert
                .AlertType
                .INFORMATION,
                LocaleManager
                .getBundle()
                .getString("class.success.downloadComplete"),
                MessageFormat
                .format(LocaleManager
                .getBundle()
                .getString("class.success.downloadSaved"),
                destination
                .getAbsolutePath()));
          });
        } else {
          Platform.runLater(() -> {
            setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
            showAlert(Alert
                .AlertType
                .ERROR,
                LocaleManager
                .getBundle()
                .getString("class.error.downloadFailed"),
                MessageFormat
                .format(LocaleManager
                .getBundle()
                .getString(ERROR_SERVER_RESPONSE),
                response
                .statusCode()));
          });
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        Platform.runLater(() -> {
          setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
          showAlert(Alert.AlertType.ERROR,
                          LocaleManager.getBundle()
                                  .getString("class.error.downloadFailed"),
                          e
                                  .getMessage());
        });
      }
    });
  }

  private void handleDeleteMaterial(Integer fileId,
                                    String filename,
                                    Button downloadButton,
                                    Button deleteButton,
                                    Label rowStatusLabel) {
    if (!isCreator || fileId == null) {
      return;
    }

    setRowActionState(downloadButton, deleteButton, rowStatusLabel, true,
                LocaleManager.getBundle().getString("class.status.deleting"));
    runAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/materials/" + fileId))
                        .header(AUTHORIZATION, BEARER + token)
                        .DELETE()
                        .build();

        HttpResponse<String> response = httpClient
                    .send(request, HttpResponse.BodyHandlers.ofString());

        Platform.runLater(() -> {
          setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
          if (response.statusCode() == 200) {
            showAlert(Alert.AlertType.INFORMATION, LocaleManager
                                .getBundle().getString(SUCCESS_TITLE),
                            MessageFormat.format(LocaleManager
                                    .getBundle().getString("class.success.deleted"), filename));
            loadMaterials();
          } else {
            showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle()
                                    .getString("class.error.deleteFailed"),
                            extractApiMessage(response.body(), MessageFormat
                                   .format(LocaleManager.getBundle()
                                            .getString(ERROR_SERVER_RESPONSE), response
                                            .statusCode())));
          }
        });
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        Platform.runLater(() -> {
          setRowActionState(downloadButton, deleteButton, rowStatusLabel, false, "");
          showAlert(Alert.AlertType.ERROR, LocaleManager
                  .getBundle().getString("class.error.deleteFailed"), e.getMessage());
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

  private String normalizeMaterialType(String type) {
    if (type == null || type.isBlank()) {
      return "other";
    }

    String normalized = type.trim().toLowerCase().replace('_', ' ').replace('-', ' ');
    if (normalized.contains("lecture") && normalized.contains("note")) {
      return "lecture notes";
    }
    if (normalized.contains("assignment")) {
      return "assignment";
    }
    if (normalized.contains("slides")) {
      return "slides";
    }
    if (normalized.contains("reference")) {
      return "reference";
    }
    return "other";
  }

  private String localizeMaterialType(String type) {
    return switch (normalizeMaterialType(type)) {
      case "lecture notes" -> LocaleManager.getString("class.materialType.lectureNotes");
      case "assignment" -> LocaleManager.getString("class.materialType.assignment");
      case "slides" -> LocaleManager.getString("class.materialType.slides");
      case "reference" -> LocaleManager.getString("class.materialType.reference");
      default -> LocaleManager.getString("class.materialType.other");
    };
  }

  private String toApiMaterialType(String selectedType) {
    return switch (normalizeMaterialType(selectedType)) {
      case "lecture notes" -> MATERIAL_LECTURE_NOTES;
      case "assignment" -> MATERIAL_ASSIGNMENT;
      case "slides" -> MATERIAL_SLIDES;
      case "reference" -> MATERIAL_REFERENCE;
      default -> MATERIAL_OTHER;
    };
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

  private void setRowActionState(Button downloadButton,
                                 Button deleteButton,
                                 Label rowStatusLabel,
                                 boolean busy,
                                 String message) {
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
    } catch (JsonSyntaxException e) {
      // handle JSON parsing issue only
    }
    return fallback;
  }

  @FXML
  private void handleDeleteClass() {
    if (!isCreator) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
          LocaleManager.getBundle().getString("class.error.onlyCreatorDelete"));
      return;
    }

    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDialog.setTitle(LocaleManager.getBundle()
            .getString("class.confirm.delete.title"));
    confirmDialog.setHeaderText(LocaleManager.getBundle()
            .getString("class.confirm.delete.header"));
    confirmDialog.setContentText(LocaleManager.getBundle()
            .getString("class.confirm.delete.content"));

    if (confirmDialog.showAndWait().isEmpty() || confirmDialog.getResult() != ButtonType.OK) {
      return;
    }

    runAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/courses/" + classId))
            .header(AUTHORIZATION, BEARER + token)
            .DELETE()
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse
                .BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 204) {
          Platform.runLater(() -> {
            showAlert(Alert.AlertType.INFORMATION, LocaleManager.getBundle()
                            .getString(SUCCESS_TITLE),
                LocaleManager.getBundle().getString("class.success.classDeleted"));
            SceneManager.loadHome();
          });
        } else {
          Platform.runLater(() -> showAlert(Alert
                          .AlertType.ERROR, LocaleManager
                          .getBundle().getString(ERROR_TITLE),
              MessageFormat.format(LocaleManager.getBundle()
                      .getString("class.error.classDeleteFailed"), response.statusCode())));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        Platform.runLater(() -> showAlert(Alert
                        .AlertType.ERROR, LocaleManager.getBundle()
                        .getString(ERROR_TITLE),
            MessageFormat.format(LocaleManager.getBundle()
                    .getString("class.error.classDeleteFailedDetailed"), e.getMessage())));
      }
    });
  }

  @FXML
  private void handleLeaveClass() {
    if (!isEnrolled || isCreator) {
      showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle().getString(ERROR_TITLE),
          LocaleManager.getBundle().getString("class.error.notEnrolled"));
      return;
    }

    Alert confirmDialog = new Alert(Alert.AlertType.CONFIRMATION);
    confirmDialog.setTitle(LocaleManager.getBundle()
            .getString("class.confirm.leave.title"));
    confirmDialog.setHeaderText(LocaleManager.getBundle()
            .getString("class.confirm.leave.header"));
    confirmDialog.setContentText(LocaleManager.getBundle()
            .getString("class.confirm.leave.content"));
    if (confirmDialog.showAndWait().isEmpty() || confirmDialog.getResult() != ButtonType.OK) {
      return;
    }

    runAsync(() -> {
      try {
        JsonObject unenrollData = new JsonObject();
        unenrollData.addProperty(USER_ID, userId);
        unenrollData.addProperty("classId", classId);

        HttpRequest request = HttpRequest.newBuilder()
            .uri(apiUri("/api/participants/unenroll"))
            .header(AUTHORIZATION, BEARER + token)
            .header("Content-Type", "application/json")
            .method("DELETE", HttpRequest.BodyPublishers.ofString(unenrollData.toString()))
            .build();

        HttpResponse<String> response = httpClient
                .send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200 || response.statusCode() == 204) {
          Platform.runLater(() -> {
            showAlert(Alert.AlertType.INFORMATION, LocaleManager.getBundle()
                            .getString(SUCCESS_TITLE),
                LocaleManager.getBundle().getString("class.success.leftClass"));
            SceneManager.loadHome();
          });
        } else {
          Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle()
                          .getString(ERROR_TITLE),
              MessageFormat.format(LocaleManager.getBundle().getString(
                      "class.error.leaveFailed"), response.statusCode())));
        }
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      } catch (Exception e) {
        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, LocaleManager.getBundle()
                        .getString(ERROR_TITLE),
            MessageFormat.format(LocaleManager.getBundle().getString(
                    "class.error.leaveFailedDetailed"), e.getMessage())));
      }
    });
  }

  private void showAlert(Alert.AlertType type, String title, String message) {
    Alert alert = new Alert(type);
    alert.setTitle(title);
    alert.setHeaderText(null);
    alert.setContentText(message);
    alert.showAndWait();
  }

  private record ReviewSubmitResult(boolean success, String message) { }

  private record ReviewsFetchResult(String message, List<String> reviewLines) { }
}

