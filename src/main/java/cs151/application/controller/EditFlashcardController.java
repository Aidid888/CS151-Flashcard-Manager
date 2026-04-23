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

/**
 * Controller for the Edit Flashcard popup window.
 * Populates the form with an existing flashcard's data and handles
 * saving updates to the front text, back text, and status in the database.
 */
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
     * Populates all form fields with data from the selected flashcard.
     * Displays read-only fields (deck name, creation date, last reviewed).
     * Called by ListFlashcardController before the popup is shown.
     *
     * @param card the flashcard whose data will be displayed and edited
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

    /**
     * Sets a callback to run after the flashcard is successfully saved.
     * Used to notify the calling controller to refresh its flashcard list.
     *
     * @param callback the Runnable to execute after a successful save
     */
    public void setOnSaved(Runnable callback) {
        this.onSaved = callback;
    }

    /**
     * Handles saving the edited flashcard when the Save button is clicked.
     * Validates input fields and saves the edited flashcard to the database.
     * Runs the onSaved callback and closes the window on success,
     * or displays an error alert on failure.
     */
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

    /**
     * Closes the popup window without saving any changes.
     */
    @FXML
    private void onCancel() {
        closeWindow();
    }

    /**
     * Closes the current popup window.
     */
    private void closeWindow() {
        ((Stage) frontTextArea.getScene().getWindow()).close();
    }

    /**
     * Displays a modal error alert dialog to the user.
     *
     * @param title   the title text of the alert window
     * @param message the error message displayed in the alert body
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }
}