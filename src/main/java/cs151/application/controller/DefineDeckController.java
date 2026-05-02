package cs151.application.controller;

import cs151.application.Main;
import javafx.beans.binding.Bindings;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import cs151.application.database.DeckDao;

/**
 * Controller for Define Deck page.
 * Handles user input for creating a new flashcard deck, including
 * input validation and saving the deck to the database.
 */
public class DefineDeckController {

    @FXML
    private TextField deckNameField;

    @FXML
    private TextArea descriptionField;

    @FXML
    private Button createDeckBtn;

    @FXML
    private Label promptMsgLbl;

    private final DeckDao deckDao = new DeckDao();
    private static final Logger logger = LoggerFactory.getLogger(DefineDeckController.class);

    /**
     * Handles initialization once page loads.
     * Disables the Create Deck button until the text field is filled by the user.
     */
    public void initialize() {
        createDeckBtn.disableProperty().bind(
                Bindings.createBooleanBinding(
                        () -> deckNameField.getText().trim().isEmpty(),
                        deckNameField.textProperty()
                )
        );
        descriptionField.setWrapText(true);
    }

    /**
     * The operation returns the user back to homepage.
     * Displays an error message if navigation fails.
     *
     * @param event the action event triggered by clicking the back button
     */
    @FXML
    protected void goBackHomeOp(ActionEvent event) {
        try {
            if (!(event.getSource() instanceof Node source)) return;
            Scene currentScene = source.getScene();
            if (currentScene == null) return;
            Stage stage = (Stage) currentScene.getWindow();

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 600);

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            promptMsgLbl.setText("Navigation error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * The operation handles new deck creation.
     * Validates the name is non-empty and unique before inserting the
     * deck into the database. Displays a respective success or error message.
     *
     * @param event the action event triggered by clicking the Create Deck button
     */
    @FXML
    private void createDeckOp(ActionEvent event) {
        promptMsgLbl.setText("");
        try {
            String name = deckNameField.getText().trim();
            String description = descriptionField.getText().trim();

            // --- Validate: name cannot be empty ---
            if (name.isEmpty()) {
                showError("Deck name cannot be empty.");
                return;
            }

            // --- Validate: name length ---
            if (name.length() > 255) {
                showError("Deck name must be 255 characters or fewer.");
                return;
            }

            // --- Validate: name must be unique ---
            if (deckDao.getDeckByName(name) != null) {
                showError("A deck named \"" + name + "\" already exists.");
                return;
            }

            // --- Save to database ---
            deckDao.insertDeck(name, description.isEmpty() ? null : description);

            showSuccess("✓ New Deck \"" + name + "\" successfully created");
        } catch (Exception e) {
            showError("Error: " + e.getMessage());
            logger.error("Failed to create deck: {}", e.getMessage(), e);
        }
    }

    private void showError(String message) {
        promptMsgLbl.setText(message);
        promptMsgLbl.getStyleClass().setAll("prompt-label", "error-label");
    }

    private void showSuccess(String message) {
        promptMsgLbl.setText(message);
        promptMsgLbl.getStyleClass().setAll("prompt-label", "success-label");
    }


}