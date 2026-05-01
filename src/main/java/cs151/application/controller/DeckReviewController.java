package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import cs151.application.util.AlertHelper;
import cs151.application.util.DateTimeUtil;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Deck Review page.
 * Handles reviewing flashcards in a deck with filtering, navigation, and editing capabilities.
 */
public class DeckReviewController {

    // Read-only fields
    @FXML private TextField deckNameField;
    @FXML private TextField creationDateField;
    @FXML private TextField lastReviewedField;

    // Editable fields
    @FXML private TextArea frontTextArea;
    @FXML private TextArea backTextArea;
    @FXML private ComboBox<String> statusComboBox;

    // Filter
    @FXML private ComboBox<String> filterComboBox;

    // Navigation buttons
    @FXML private Button previousBtn;
    @FXML private Button nextBtn;
    @FXML private Button saveBtn;

    // Status label
    @FXML private Label statusLabel;
    @FXML private Label cardCountLabel;

    private final FlashcardDao flashcardDao = new FlashcardDao();
    private Deck currentDeck;
    private List<Flashcard> filteredFlashcards;
    private int currentIndex;
    private Flashcard currentFlashcard;
    private boolean isModified = false;

    /**
     * Initializes the UI components when the page loads.
     */
    @FXML
    public void initialize() {
        // Setup filter ComboBox
        filterComboBox.setItems(FXCollections.observableArrayList(
                "All", "New", "Learning", "Mastered"));
        filterComboBox.getSelectionModel().selectFirst();

        // Setup status ComboBox
        statusComboBox.setItems(FXCollections.observableArrayList(
                "New", "Learning", "Mastered"));

        // Make read-only fields non-editable
        deckNameField.setEditable(false);
        creationDateField.setEditable(false);
        lastReviewedField.setEditable(false);

        // Style read-only fields
        String readOnlyStyle = "-fx-background-color: derive(-fx-base, 10%);" +
                "-fx-text-fill: -fx-text-inner-color;" +
                "-fx-opacity: 0.75;";
        deckNameField.setStyle(readOnlyStyle);
        creationDateField.setStyle(readOnlyStyle);
        lastReviewedField.setStyle(readOnlyStyle);

        // Track modifications
        frontTextArea.textProperty().addListener((obs, oldVal, newVal) -> isModified = true);
        backTextArea.textProperty().addListener((obs, oldVal, newVal) -> isModified = true);
        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> isModified = true);

        // Wrap text in TextAreas
        frontTextArea.setWrapText(true);
        backTextArea.setWrapText(true);
    }

    /**
     * Sets the deck to review and loads its flashcards.
     * Called by ReviewController when navigating to this page.
     *
     * @param deck the deck whose flashcards will be reviewed
     */
    public void setDeck(Deck deck) {
        this.currentDeck = deck;
        deckNameField.setText(deck.deckName());
        currentIndex = 0;
        loadFilteredFlashcards();
    }

    /**
     * Loads flashcards based on the current filter selection.
     */
    private void loadFilteredFlashcards() {
        try {
            String filter = filterComboBox.getValue();
            if (filter == null) filter = "All";

            if ("All".equals(filter)) {
                filteredFlashcards = flashcardDao.getFlashcardsByDeckId(currentDeck.id());
            } else {
                filteredFlashcards = flashcardDao.getFlashcardsByStatus(currentDeck.id(), filter);
            }

            if (filteredFlashcards.isEmpty()) {
                statusLabel.setText("No flashcards found for filter: " + filter);
                cardCountLabel.setText("0 / 0");
                clearFields();
                disableNavigation();
                return;
            }

            // Ensure currentIndex is valid
            if (currentIndex >= filteredFlashcards.size()) {
                currentIndex = 0;
            }

            displayFlashcard();

        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load flashcards:\n" + e.getMessage());
        }
    }

    /**
     * Displays the current flashcard in the UI and updates navigation buttons.
     */
    private void displayFlashcard() {
        if (filteredFlashcards == null || filteredFlashcards.isEmpty()) {
            return;
        }

        currentFlashcard = filteredFlashcards.get(currentIndex);

        // Update read-only fields
        creationDateField.setText(DateTimeUtil.utcToLocal(currentFlashcard.creationDate()));

        // Update last reviewed to current date/time
        LocalDateTime now = LocalDateTime.now();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        lastReviewedField.setText(now.format(formatter));

        // Update editable fields
        frontTextArea.setText(currentFlashcard.frontText());
        backTextArea.setText(currentFlashcard.backText());
        statusComboBox.setValue(currentFlashcard.status());

        // Update card count
        cardCountLabel.setText((currentIndex + 1) + " / " + filteredFlashcards.size());

        // Update navigation buttons
        previousBtn.setDisable(currentIndex == 0);
        nextBtn.setDisable(currentIndex == filteredFlashcards.size() - 1);

        // Clear status and modification flag
        statusLabel.setText("");
        isModified = false;

        // Update last_viewed timestamp in database
        try {
            flashcardDao.updateLastViewed(currentFlashcard.id());
        } catch (SQLException e) {
            // Log but don't interrupt the user experience
            System.err.println("Failed to update last_viewed: " + e.getMessage());
        }
    }

    /**
     * Clears all editable fields when no flashcards are available.
     */
    private void clearFields() {
        frontTextArea.clear();
        backTextArea.clear();
        statusComboBox.getSelectionModel().clearSelection();
        creationDateField.clear();
        lastReviewedField.clear();
    }

    /**
     * Disables navigation and save buttons when no flashcards are available.
     */
    private void disableNavigation() {
        previousBtn.setDisable(true);
        nextBtn.setDisable(true);
        saveBtn.setDisable(true);
    }

    /**
     * Handles filter selection change.
     * Reloads flashcards based on the new filter.
     */
    @FXML
    private void onFilterChanged() {
        if (isModified) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save before changing the filter?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveCurrentFlashcard();
                }
            });
        }

        currentIndex = 0;
        loadFilteredFlashcards();
    }

    /**
     * Navigates to the next flashcard.
     */
    @FXML
    private void onNext() {
        if (isModified) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save before moving to the next card?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveCurrentFlashcard();
                }
            });
        }

        if (currentIndex < filteredFlashcards.size() - 1) {
            currentIndex++;
            displayFlashcard();
        }
    }

    /**
     * Navigates to the previous flashcard.
     */
    @FXML
    private void onPrevious() {
        if (isModified) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save before moving to the previous card?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveCurrentFlashcard();
                }
            });
        }

        if (currentIndex > 0) {
            currentIndex--;
            displayFlashcard();
        }
    }

    /**
     * Saves the current flashcard's modifications to the database.
     */
    @FXML
    private void onSave() {
        saveCurrentFlashcard();
    }

    /**
     * Internal method to save the current flashcard.
     */
    private void saveCurrentFlashcard() {
        if (currentFlashcard == null) {
            return;
        }

        String front = frontTextArea.getText().trim();
        String back = backTextArea.getText().trim();
        String status = statusComboBox.getValue();

        // Validation
        if (front.isEmpty() || back.isEmpty() || status == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Front text, back text, and status are required.");
            return;
        }

        try {
            flashcardDao.updateFlashcard(currentFlashcard.id(), front, back, status);

            // Update the current flashcard object
            currentFlashcard = new Flashcard(
                    currentFlashcard.id(),
                    currentFlashcard.deckId(),
                    currentFlashcard.deckName(),
                    front,
                    back,
                    status,
                    currentFlashcard.creationDate(),
                    currentFlashcard.lastViewed()
            );

            // Update in the filtered list
            filteredFlashcards.set(currentIndex, currentFlashcard);

            statusLabel.setText("✓ Flashcard saved successfully!");
            isModified = false;

        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not save flashcard:\n" + e.getMessage());
        }
    }

    /**
     * Navigates back to the Review page (deck list).
     */
    @FXML
    private void onCancel(ActionEvent event) {
        if (isModified) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save before leaving?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveCurrentFlashcard();
                }
            });
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/review-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not return to review page:\n" + e.getMessage());
        }
    }

    /**
     * Navigates back to the home page.
     */
    @FXML
    private void goBackHomeOp(ActionEvent event) {
        if (isModified) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Unsaved Changes");
            confirm.setHeaderText("You have unsaved changes");
            confirm.setContentText("Do you want to save before leaving?");
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    saveCurrentFlashcard();
                }
            });
        }

        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not return home:\n" + e.getMessage());
        }
    }
}