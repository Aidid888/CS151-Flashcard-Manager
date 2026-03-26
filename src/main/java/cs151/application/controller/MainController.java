package cs151.application.controller;

import cs151.application.Main;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Label;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.MenuButton;

public class MainController {
    @FXML
    private Label flashcardTitle;

    /**
     * The operation instantiates homepage once the program is initialized.
     */
    @FXML
    public void initialize() {
        flashcardTitle.setText("Flashcard Manager");
    }

    /**
     * The operation redirects the user to the Define Deck page.
     */
    @FXML
    protected void goToDefineDeckOp(ActionEvent event) {
        try {

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/define-deck-view.fxml"));

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
     * The operation redirects the user to the Define Flashcards page.
     */
    public void goToDefineFlashcardsOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/define-flashcard-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage)((Node)event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    @FXML
    private MenuButton viewEditMenuBtn;

    /**
     * The operation redirects the user to the List Deck page.
     */

    @FXML
    protected void goToListDecksOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-decks-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage) viewEditMenuBtn.getScene().getWindow(); // ← changed

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    /**
     * The operation redirects the user to the List Flashcards page.
     */
    @FXML
    protected void goToListFLashcardsOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-flashcards-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage) viewEditMenuBtn.getScene().getWindow(); // ← changed

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation terminates the program.
     */
    @FXML
    protected void onCloseButtonClickOp() {
        Platform.exit();
    }

}