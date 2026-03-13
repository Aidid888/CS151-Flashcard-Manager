package cs151.application;

import javafx.fxml.FXML;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;

/**
 * Controller for Define Deck page.
 */
public class DefineDeckController {

    @FXML
    private TextField deckNameField;

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea descriptionField;

    /**
     * Handles deck creation.
     */
    @FXML
    private void createDeck() {

        String name = deckNameField.getText();
        String subject = subjectField.getText();
        String description = descriptionField.getText();

        System.out.println("Deck Created:");
        System.out.println("Name: " + name);
        System.out.println("Subject: " + subject);
        System.out.println("Description: " + description);
    }
}