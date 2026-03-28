package com.example.app;

import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

public class ReviewDialogController {

    private static final int COMMENT_MAX_LENGTH = 500;

    @FXML private Label materialNameLabel;
    @FXML private ComboBox<Integer> ratingCombo;
    @FXML private TextArea commentTextArea;
    @FXML private Label validationLabel;

    private boolean submitted;
    private Integer selectedRating;
    private String comment;

    @FXML
    private void initialize() {
        ratingCombo.getItems().setAll(1, 2, 3, 4, 5);
        ratingCombo.getSelectionModel().select(Integer.valueOf(5));
        validationLabel.setText("");
    }

    public void setMaterialName(String name) {
        materialNameLabel.setText(name == null || name.isBlank() ? "Unnamed file" : name);
    }

    public boolean isSubmitted() { return submitted; }
    public Integer getSelectedRating() { return selectedRating; }
    public String getComment() { return comment; }

    @FXML
    private void handleSubmit() {
        Integer rating = ratingCombo.getValue();
        String trimmed = commentTextArea.getText() == null ? "" : commentTextArea.getText().trim();

        if (rating == null || rating < 1 || rating > 5) { validationLabel.setText("Choose a rating between 1 and 5."); return; }
        if (trimmed.isBlank()) { validationLabel.setText("Please add a comment."); return; }
        if (trimmed.length() > COMMENT_MAX_LENGTH) { validationLabel.setText("Comment must be max " + COMMENT_MAX_LENGTH + " characters."); return; }

        this.selectedRating = rating;
        this.comment = trimmed;
        this.submitted = true;
        ((Stage) commentTextArea.getScene().getWindow()).close();
    }

    @FXML
    private void handleCancel() {
        this.submitted = false;
        ((Stage) commentTextArea.getScene().getWindow()).close();
    }
}

