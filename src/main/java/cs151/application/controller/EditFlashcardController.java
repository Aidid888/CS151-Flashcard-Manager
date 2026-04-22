package cs151.application.controller;

import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;

import java.sql.SQLException;

public class EditFlashcardController {

    @FXML private TextArea frontTextArea;
    @FXML private TextArea backTextArea;
    @FXML private ComboBox<String> statusComboBox;

    private Flashcard flashcard;
    private Runnable onSaved;
    private final FlashcardDao flashcardDao = new FlashcardDao();

    @FXML
    public void initialize() {
        // Must match the CHECK constraint in the DB schema exactly
        statusComboBox.setItems(FXCollections.observableArrayList(
                "New", "Learning", "Mastered"
        ));
    }

    public void setFlashcard(Flashcard card) {
        this.flashcard = card;
        frontTextArea.setText(card.frontText());
        backTextArea.setText(card.backText());
        statusComboBox.setValue(card.status());
    }

    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    @FXML
    private void onSave() {
        try {
            // updateFlashcard already exists in your FlashcardDao — no changes needed there
            flashcardDao.updateFlashcard(
                    flashcard.id(),
                    frontTextArea.getText(),
                    backTextArea.getText(),
                    statusComboBox.getValue()
            );
            if (onSaved != null) onSaved.run();
            closeWindow();
        } catch (SQLException e) {
            javafx.scene.control.Alert alert =
                    new javafx.scene.control.Alert(
                            javafx.scene.control.Alert.AlertType.ERROR,
                            "Could not save flashcard:\n" + e.getMessage(),
                            javafx.scene.control.ButtonType.OK
                    );
            alert.setTitle("Database Error");
            alert.setHeaderText(null);
            alert.showAndWait();
        }
    }

    @FXML
    private void onCancel() {
        closeWindow();
    }

    private void closeWindow() {
        ((Stage) frontTextArea.getScene().getWindow()).close();
    }
}