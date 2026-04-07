package com.example.app;

import java.util.List;
import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.stage.Stage;

public class ViewReviewsDialogController {

  @FXML
  private Label materialNameLabel;

  @FXML
  private Label statusLabel;

  @FXML
  private ListView<String> reviewsListView;

  public void setMaterialName(String name) {
    materialNameLabel.setText(name == null || name.isBlank() ? "Unnamed file" : name);
  }

  public void setStatus(String status) {
    statusLabel.setText(status == null ? "" : status);
  }

  public void setReviewLines(List<String> lines, String status) {
    reviewsListView.getItems().clear();
    if (lines != null && !lines.isEmpty()) {
      reviewsListView.getItems().addAll(lines);
    }
    statusLabel.setText(status == null ? "" : status);
  }

  @FXML
  private void handleClose() {
    Stage stage = (Stage) statusLabel.getScene().getWindow();
    stage.close();
  }
}

