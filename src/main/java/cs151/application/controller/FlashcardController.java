package cs151.application.controller;

import cs151.application.Main;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.stage.Stage;


public class FlashcardController {

    @FXML
    public void initialize() {}

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
}
