package cs151.application;

import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;

public class MainController {
    @FXML
    private Label flashcardTitle;

    @FXML
    public void initialize() {
        flashcardTitle.setText("Flashcard Manager");
    }

    @FXML
    protected void goToDefineDeckOp(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("define-deck-view.fxml"));

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

}