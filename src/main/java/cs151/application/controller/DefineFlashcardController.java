package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.text.Text;
import javafx.scene.layout.VBox;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.sql.SQLException;
import java.util.List;

/**
 * Controller for the Define Flashcard view.
 * Handles user input for creating a new flashcard, including deck selection,
 * field validation, duplicate checking, and saving to the database.
 */
public class DefineFlashcardController {

    // ── FXML fields ───────────────────────────────────────────────
    @FXML private ComboBox<Deck>   deckComboBox;
    @FXML private TextArea         frontTextArea;
    @FXML private TextArea         backTextArea;
    @FXML private ComboBox<String> statusComboBox;

    @FXML private Label deckErrorLabel;
    @FXML private Label frontErrorLabel;
    @FXML private Label backErrorLabel;
    @FXML private Label statusErrorLabel;
    @FXML private Label successLabel;

    @FXML private VBox  deckDescriptionBox;
    @FXML private Text deckDescriptionText;

    // ── DAOs ──────────────────────────────────────────────────────
    private final DeckDao      deckDao      = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();

    // ── Initialization ────────────────────────────────────────────

    /**
     * Handles initialization once page loads.
     * Populates the deck and status ComboBoxes, configures deck name rendering,
     * and sets up a listener to show or hide the deck description when a deck is selected.
     */
    @FXML
    public void initialize() {
        // Deck ComboBox — display deck names instead of Object.toString()
        deckComboBox.setCellFactory(lv -> new DeckCell());
        deckComboBox.setButtonCell(new DeckCell());
        loadDeckComboBox();

        // Status ComboBox
        statusComboBox.setItems(FXCollections.observableArrayList(
                "New", "Learning", "Mastered"));
        statusComboBox.getSelectionModel().selectFirst();

        // Show deck description whenever the selection changes
        deckComboBox.valueProperty().addListener((obs, oldDeck, newDeck) -> {
            if (newDeck == null || newDeck.description() == null
                    || newDeck.description().isBlank()) {
                deckDescriptionBox.setVisible(false);
                deckDescriptionBox.setManaged(false);
            } else {
                deckDescriptionText.setText(newDeck.description()); // ✅ was deckDescriptionLabel
                deckDescriptionBox.setVisible(true);
                deckDescriptionBox.setManaged(true);
            }
        });
    }

    /**
     * Automatically pre-selects deck in the ComboBox when navigating
     * from a specific deck row. Called by ListDeckController.
     */
    public void setDeck(Deck deck) {
        deckComboBox.getItems().stream()
                .filter(d -> d.id() == deck.id())
                .findFirst()
                .ifPresent(d -> deckComboBox.getSelectionModel().select(d));
    }

    // ── Handlers ──────────────────────────────────────────────────

    /**
     * Handles saving a new flashcard when the Save button is clicked.
     * Validates fields, checks front text uniqueness within the selected deck,
     * then inserts the flashcard into the database. Displays corresponding
     * success or error message.
     */
    @FXML
    private void onSaveFlashcard() {
        if (!validate()) return;

        Deck selectedDeck = deckComboBox.getValue();
        String frontText  = frontTextArea.getText().trim();
        String backText   = backTextArea.getText().trim();
        String status     = statusComboBox.getValue();

        try {
            if (flashcardDao.existsByFrontText(selectedDeck.id(), frontText)) {
                frontErrorLabel.setText("A flashcard with this front text already exists in this deck.");
                return;
            }

            flashcardDao.insertFlashcard(
                    selectedDeck.id(),
                    frontText, backText, status);

            clearFields();
            successLabel.setText("✓ Flashcard saved successfully!");

        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save flashcard:\n" + e.getMessage());
        }
    }

    @FXML
    private void onClearFields() {
        clearFields();
        successLabel.setText("");
    }

    /**
     * Operation returns the user back to the home page.
     *
     * @param event the action event triggered by clicking the back button
     */
    @FXML
    protected void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    /**
     * Fetches all decks from the database and populates the deck ComboBox.
     * Displays an error alert if the database query fails.
     */
    private void loadDeckComboBox() {
        try {
            deckComboBox.setItems(
                    FXCollections.observableArrayList(deckDao.getAllDecks()));
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load decks:\n" + e.getMessage());
        }
    }

    /**
     * Validates all required input fields and displays inline error labels for any failures.
     *
     * @return true if all fields are valid, false if any validation check fails
     */
    private boolean validate() {
        boolean valid = true;

        deckErrorLabel.setText("");
        frontErrorLabel.setText("");
        backErrorLabel.setText("");
        statusErrorLabel.setText("");
        successLabel.setText("");

        if (deckComboBox.getValue() == null) {
            deckErrorLabel.setText("Please select a deck.");
            valid = false;
        }

        if (frontTextArea.getText().trim().isEmpty()) {
            frontErrorLabel.setText("Front text is required.");
            valid = false;
        }

        if (backTextArea.getText().trim().isEmpty()) {
            backErrorLabel.setText("Back text is required.");
            valid = false;
        }

        if (statusComboBox.getValue() == null) {
            statusErrorLabel.setText("Please select a status.");
            valid = false;
        }

        return valid;
    }

    /**
     * Clears all input fields and error labels to their default state.
     * Also hides the deck description box.
     */
    private void clearFields() {
        frontTextArea.clear();
        backTextArea.clear();
        statusComboBox.getSelectionModel().selectFirst();
        deckErrorLabel.setText("");
        frontErrorLabel.setText("");
        backErrorLabel.setText("");
        statusErrorLabel.setText("");
        deckDescriptionBox.setVisible(false);
        deckDescriptionBox.setManaged(false);
    }

    /**
     * Custom ListCell displays a Deck's name in the ComboBox
     * instead of the default Object.toString() output.
     * Private static class contains single protected method.
     */
    private static class DeckCell extends ListCell<Deck> {
        /**
         * Updates the cell text to the deck's name, or clears it if the cell is empty.
         *
         * @param deck the Deck object to display
         * @param empty true if the cell contains no data, false otherwise
         */
        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            setText(empty || deck == null ? null : deck.deckName());
        }
    }
}