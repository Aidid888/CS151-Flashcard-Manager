package cs151.application;

import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * Controller for Define Deck page.
 */
public class DefineDeckController {

    @FXML
    public Button exitHomeBtn;

    @FXML
    private TextField deckNameField;

    @FXML
    private TextField subjectField;

    @FXML
    private TextArea descriptionField;

    /**
     * Returns user back to homepage.
     */
    @FXML
    protected void onExitButtonClickOp(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("home-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 400);

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