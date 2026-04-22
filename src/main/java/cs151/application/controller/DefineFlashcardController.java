package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
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
     * Called by ListDeckController when navigating from a specific deck row.
     * Pre-selects that deck in the ComboBox automatically.
     */
    public void setDeck(Deck deck) {
        deckComboBox.getItems().stream()
                .filter(d -> d.id() == deck.id())
                .findFirst()
                .ifPresent(d -> deckComboBox.getSelectionModel().select(d));
    }

    // ── Handlers ──────────────────────────────────────────────────

    /**
     * Validates fields, checks front text uniqueness within the selected deck,
     * then inserts the flashcard into the database.
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
            showAlert("Database Error", "Could not save flashcard:\n" + e.getMessage());
        }
    }

    @FXML
    private void onClearFields() {
        clearFields();
        successLabel.setText("");
    }

    /**
     * Returns the user back to the home page.
     */
    @FXML
    protected void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ── Private helpers ───────────────────────────────────────────

    private void loadDeckComboBox() {
        try {
            deckComboBox.setItems(
                    FXCollections.observableArrayList(deckDao.getAllDecks()));
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load decks:\n" + e.getMessage());
        }
    }

    /**
     * Validates all required fields. Shows inline error labels per field.
     * Returns true only if everything passes.
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

    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Renders Deck records in the ComboBox by deck name
     * instead of the default Object.toString() output.
     */
    private static class DeckCell extends ListCell<Deck> {
        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            setText(empty || deck == null ? null : deck.deckName());
        }
    }
}