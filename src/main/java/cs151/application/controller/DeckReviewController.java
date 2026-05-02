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
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import javafx.animation.ScaleTransition;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;

import java.io.IOException;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Controller for the Deck Review page.
 * Handles filtering, flipping, editing, saving, and navigating flashcards.
 */
public class DeckReviewController {

    // Read-only fields
    @FXML private Label deckNameLabel;
    @FXML private TextField creationDateField;
    @FXML private TextField lastReviewedField;

    // Editable fields
    @FXML private TextArea frontTextArea;
    @FXML private TextArea backTextArea;
    @FXML private ComboBox<String> statusComboBox;

    // Filter
    @FXML private ComboBox<String> filterComboBox;

    // Card faces
    @FXML private StackPane cardStackPane;
    @FXML private VBox frontFace;
    @FXML private VBox backFace;

    // Buttons / labels
    @FXML private Button previousBtn;
    @FXML private Button nextBtn;
    @FXML private Button saveBtn;
    @FXML private Button flipBtn;
    @FXML private Label statusLabel;
    @FXML private Label cardCountLabel;

    private final FlashcardDao flashcardDao = new FlashcardDao();

    private Deck currentDeck;
    private List<Flashcard> filteredFlashcards;
    private Flashcard currentFlashcard;
    private int currentIndex = 0;

    private boolean isModified = false;
    private boolean isLoading = false;
    private String lastCommittedFilter = "All";

    private boolean showingFront = true;
    private boolean isFlipping = false;

    @FXML
    public void initialize() {
        filterComboBox.setItems(FXCollections.observableArrayList("All", "New", "Learning", "Mastered"));
        filterComboBox.getSelectionModel().select("All");

        statusComboBox.setItems(FXCollections.observableArrayList("New", "Learning", "Mastered"));

        creationDateField.setEditable(false);
        lastReviewedField.setEditable(false);
        frontTextArea.setWrapText(true);
        backTextArea.setWrapText(true);

        frontTextArea.textProperty().addListener((obs, oldVal, newVal) -> markModified());
        backTextArea.textProperty().addListener((obs, oldVal, newVal) -> markModified());
        statusComboBox.valueProperty().addListener((obs, oldVal, newVal) -> markModified());

        setFaceVisible(true);

        cardStackPane.setOnMouseClicked(this::onCardClicked);
    }

    /** Called by ReviewController after loading deck-review-view.fxml. */
    public void setDeck(Deck deck) {
        currentDeck = deck;
        deckNameLabel.setText(deck.deckName());
        currentIndex = 0;
        loadFilteredFlashcards();
    }

    private void markModified() {
        if (!isLoading) {
            isModified = true;
            statusLabel.setText("");
        }
    }

    private void loadFilteredFlashcards() {
        if (currentDeck == null) return;

        try {
            String filter = filterComboBox.getValue() == null ? "All" : filterComboBox.getValue();

            filteredFlashcards = "All".equals(filter)
                    ? flashcardDao.getFlashcardsByDeckId(currentDeck.id())
                    : flashcardDao.getFlashcardsByStatus(currentDeck.id(), filter);

            if (filteredFlashcards.isEmpty()) {
                clearFields();
                setReviewControlsDisabled(true);
                cardCountLabel.setText("0 / 0");
                statusLabel.setText("No flashcards found for filter: " + filter);
                return;
            }

            currentIndex = Math.max(0, Math.min(currentIndex, filteredFlashcards.size() - 1));
            displayFlashcard();

        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load flashcards:\n" + e.getMessage());
        }
    }

    private void displayFlashcard() {
        currentFlashcard = filteredFlashcards.get(currentIndex);
        setReviewControlsDisabled(false);
        showingFront = true;
        setFaceVisible(true);

        isLoading = true;
        frontTextArea.setText(currentFlashcard.frontText());
        backTextArea.setText(currentFlashcard.backText());
        statusComboBox.setValue(currentFlashcard.status());
        creationDateField.setText(formatDate(currentFlashcard.creationDate()));
        lastReviewedField.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
        isLoading = false;

        cardCountLabel.setText((currentIndex + 1) + " / " + filteredFlashcards.size());
        previousBtn.setDisable(currentIndex == 0);
        nextBtn.setDisable(currentIndex == filteredFlashcards.size() - 1);

        statusLabel.setText("");
        isModified = false;

        try {
            flashcardDao.updateLastViewed(currentFlashcard.id());
        } catch (SQLException e) {
            System.err.println("Could not update last reviewed date: " + e.getMessage());
        }
    }

    private String formatDate(String utcDate) {
        return utcDate == null ? "" : DateTimeUtil.utcToLocal(utcDate);
    }

    private void clearFields() {
        isLoading = true;
        frontTextArea.clear();
        backTextArea.clear();
        statusComboBox.getSelectionModel().clearSelection();
        creationDateField.clear();
        lastReviewedField.clear();
        isLoading = false;

        currentFlashcard = null;
        isModified = false;
        setFaceVisible(true);
    }

    private void setReviewControlsDisabled(boolean disabled) {
        frontTextArea.setDisable(disabled);
        backTextArea.setDisable(disabled);
        statusComboBox.setDisable(disabled);
        saveBtn.setDisable(disabled);
        flipBtn.setDisable(disabled);
        previousBtn.setDisable(disabled || currentIndex == 0);
        nextBtn.setDisable(disabled || filteredFlashcards == null || currentIndex >= filteredFlashcards.size() - 1);
    }

    @FXML
    public void flipCard() {
        if (isFlipping || filteredFlashcards == null || filteredFlashcards.isEmpty()) {
            return;
        }

        isFlipping = true;

        ScaleTransition shrink = new ScaleTransition(Duration.millis(140), cardStackPane);
        shrink.setFromX(1.0);
        shrink.setToX(0.0);

        shrink.setOnFinished(e -> {
            showingFront = !showingFront;
            setFaceVisible(showingFront);

            ScaleTransition expand = new ScaleTransition(Duration.millis(140), cardStackPane);
            expand.setFromX(0.0);
            expand.setToX(1.0);
            expand.setOnFinished(ev -> isFlipping = false);
            expand.play();
        });

        shrink.play();
    }

    private void setFaceVisible(boolean showFront) {
        frontFace.setVisible(showFront);
        frontFace.setManaged(showFront);
        backFace.setVisible(!showFront);
        backFace.setManaged(!showFront);
    }

    @FXML
    private void onFilterChanged() {
        if (isLoading || currentDeck == null) return;
        if (!confirmSaveIfModified("changing the filter")) return;

        lastCommittedFilter = filterComboBox.getValue();
        currentIndex = 0;
        loadFilteredFlashcards();
    }

    @FXML
    private void onNext() {
        if (!confirmSaveIfModified("moving to the next card")) return;

        if (filteredFlashcards != null && currentIndex < filteredFlashcards.size() - 1) {
            currentIndex++;
            displayFlashcard();
        }
    }

    @FXML
    private void onPrevious() {
        if (!confirmSaveIfModified("moving to the previous card")) return;

        if (filteredFlashcards != null && currentIndex > 0) {
            currentIndex--;
            displayFlashcard();
        }
    }

    @FXML
    private void onSave() {
        saveCurrentFlashcard();
    }

    private boolean saveCurrentFlashcard() {
        if (currentFlashcard == null) return true;

        String front = frontTextArea.getText().trim();
        String back = backTextArea.getText().trim();
        String status = statusComboBox.getValue();
        String oldStatus = currentFlashcard.status();

        if (front.isEmpty() || back.isEmpty() || status == null) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Validation Error",
                    "Front text, back text, and status are required.");

            reloadCurrentFlashcardFromDatabase();
            return false;
        }

        try {
            flashcardDao.updateFlashcard(currentFlashcard.id(), front, back, status);

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

            boolean statusChanged = !oldStatus.equals(status);
            boolean filterIsAll = "All".equals(filterComboBox.getValue());

            // If status changed while using a specific filter,
            // reload the filtered list so the card counter updates correctly.
            if (statusChanged && !filterIsAll) {
                loadFilteredFlashcards();
                statusLabel.setText("✓ Flashcard saved. Filter refreshed.");
            } else {
                filteredFlashcards.set(currentIndex, currentFlashcard);
                cardCountLabel.setText((currentIndex + 1) + " / " + filteredFlashcards.size());
                statusLabel.setText("✓ Flashcard saved successfully!");
            }

            isModified = false;
            return true;

        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not save flashcard:\n" + e.getMessage());
            reloadCurrentFlashcardFromDatabase();
            return false;
        }
    }

    private boolean confirmSaveIfModified(String action) {
        if (!isModified) return true;

        ButtonType result = showUnsavedChangesDialog(action);

        if (result.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
            return saveCurrentFlashcard();
        }

        if (result.getButtonData() == ButtonBar.ButtonData.CANCEL_CLOSE) {
            isLoading = true;
            filterComboBox.setValue(lastCommittedFilter);
            isLoading = false;
            return false;
        }

        isModified = false; // discard changes
        return true;
    }

    private ButtonType showUnsavedChangesDialog(String action) {
        ButtonType save = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        ButtonType discard = new ButtonType("Discard", ButtonBar.ButtonData.NO);
        ButtonType cancel = new ButtonType("Cancel", ButtonBar.ButtonData.CANCEL_CLOSE);

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION,
                "Do you want to save before " + action + "?", save, discard, cancel);
        alert.setTitle("Unsaved Changes");
        alert.setHeaderText("You have unsaved changes");
        return alert.showAndWait().orElse(cancel);
    }

    private void reloadCurrentFlashcardFromDatabase() {
        if (currentFlashcard == null) return;

        try {
            Flashcard savedFlashcard = flashcardDao.getFlashcardById(currentFlashcard.id());

            if (savedFlashcard == null) {
                clearFields();
                setReviewControlsDisabled(true);
                statusLabel.setText("This flashcard no longer exists in the database.");
                return;
            }

            currentFlashcard = savedFlashcard;

            if (filteredFlashcards != null
                    && currentIndex >= 0
                    && currentIndex < filteredFlashcards.size()) {
                filteredFlashcards.set(currentIndex, savedFlashcard);
            }

            isLoading = true;
            frontTextArea.setText(savedFlashcard.frontText());
            backTextArea.setText(savedFlashcard.backText());
            statusComboBox.setValue(savedFlashcard.status());
            creationDateField.setText(formatDate(savedFlashcard.creationDate()));
            lastReviewedField.setText(formatDate(savedFlashcard.lastViewed()));
            isLoading = false;

            isModified = false;
            statusLabel.setText("Changes were not saved.");

        } catch (SQLException reloadError) {
            isLoading = true;
            frontTextArea.setText(currentFlashcard.frontText());
            backTextArea.setText(currentFlashcard.backText());
            statusComboBox.setValue(currentFlashcard.status());
            creationDateField.setText(formatDate(currentFlashcard.creationDate()));
            lastReviewedField.setText(formatDate(currentFlashcard.lastViewed()));
            isLoading = false;

            isModified = false;
            statusLabel.setText("Changes were not saved. Showing last loaded version.");
        }
    }

    @FXML
    private void onCancel(ActionEvent event) {
        if (confirmSaveIfModified("leaving")) {
            navigateTo(event, "view/review-view.fxml", "Could not return to review page");
        }
    }

    private void onCardClicked(MouseEvent event) {
        Object target = event.getTarget();
        // Only flip if the click was NOT on a TextArea (so editing still works)
        if (!(target instanceof TextArea)) {
            flipCard();
        }
    }

    @FXML
    private void goBackHomeOp(ActionEvent event) {
        if (confirmSaveIfModified("leaving")) {
            navigateTo(event, "view/home-view.fxml", "Could not return home");
        }
    }

    private void navigateTo(ActionEvent event, String fxmlPath, String errorMessage) {
        try {
            FXMLLoader loader = new FXMLLoader(Main.class.getResource(fxmlPath));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    errorMessage + ":\n" + e.getMessage());
        }
    }
}
