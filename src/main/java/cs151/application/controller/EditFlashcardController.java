package cs151.application.controller;

import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditFlashcardController {

    // Read-only fields
    @FXML private TextField deckField;
    @FXML private TextField creationDateField;
    @FXML private TextField lastReviewedField;

    // Editable fields
    @FXML private TextArea frontTextArea;
    @FXML private TextArea backTextArea;
    @FXML private ComboBox<String> statusComboBox;

    private Flashcard flashcard;
    private Runnable onSaved;
    private final FlashcardDao flashcardDao = new FlashcardDao();

    @FXML
    public void initialize() {
        statusComboBox.setItems(FXCollections.observableArrayList(
                "New", "Learning", "Mastered"
        ));
    }

    /**
     * Called by ListFlashcardController before the popup is shown.
     * Populates all fields from the given Flashcard record.
     */
    public void setFlashcard(Flashcard card) {
        this.flashcard = card;

        // Read-only fields — populated but not editable
        deckField.setText(card.deckName());
        creationDateField.setText(card.creationDate());
        lastReviewedField.setText(
                card.lastViewed() == null ? "Never" : card.lastViewed()
        );

        // Editable fields
        frontTextArea.setText(card.frontText());
        backTextArea.setText(card.backText());
        statusComboBox.setValue(card.status());
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        String front = frontTextArea.getText().trim();
        String back = backTextArea.getText().trim();
        String status = statusComboBox.getValue();

        // Basic validation
        if (front.isEmpty() || back.isEmpty() || status == null) {
            showAlert("Validation Error", "Front text, back text, and status are required.");
            return;
        }

        try {
            flashcardDao.updateFlashcard(flashcard.id(), front, back, status);
            if (onSaved != null) onSaved.run();
            closeWindow();
        } catch (SQLException e) {
            showAlert("Database Error", "Could not save flashcard:\n" + e.getMessage());
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) frontTextArea.getScene().getWindow()).close();
    }

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}