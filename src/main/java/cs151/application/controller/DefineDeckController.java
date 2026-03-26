package cs151.application.controller;

import cs151.application.Main;
import javafx.application.Platform;
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
import cs151.application.database.DeckDao;

/**
 * Controller for Define Deck page.
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

    /**
     * Handles initialization once page loads.
     * Disables the Create Deck button until the text field is filled by the user.
     */
    public void initialize() {
        createDeckBtn.disableProperty().bind(deckNameField.textProperty().isEmpty());
        descriptionField.setWrapText(true);
    }

    /**
     * The operation returns the user back to homepage.
     */
    @FXML
    protected void onExitButtonClickOp(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage)((Node)event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();

        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation handles deck creation.
     */
    @FXML
    private void createDeckOp(ActionEvent event) {
        try {
            String name = deckNameField.getText().trim();
            String description = descriptionField.getText().trim();

            // --- Validate: name cannot be empty ---
            if (name.isEmpty()) {
                promptMsgLbl.setText("Deck name cannot be empty.");
                return;
            }

            // --- Validate: name must be unique ---
            if (deckDao.getDeckByName(name) != null) {
                promptMsgLbl.setText("A deck named \"" + name + "\" already exists.");
                return;
            }

            // --- Save to database ---
            deckDao.insertDeck(name, description.isEmpty() ? null : description);

            promptMsgLbl.setText("New Deck \"" + name + "\" successfully created");

        } catch (Exception e) {
            promptMsgLbl.setText("Error: " + e.getMessage());
            System.out.println(e.getMessage());
        }
    }
}