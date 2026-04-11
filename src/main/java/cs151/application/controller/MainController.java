package cs151.application.controller;

import cs151.application.Main;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.stage.Stage;

public class MainController {

    @FXML
    private Label flashcardTitle;

    @FXML
    private MenuButton viewEditMenuBtn;

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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 400));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation redirects the user to the Define Flashcards page.
     */
    @FXML
    public void goToDefineFlashcardsOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/define-flashcard-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation redirects the user to the List Deck page.
     * Called from the View/Edit MenuButton.
     */
    @FXML
    protected void goToListDecksOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-decks-view.fxml"));
            Stage stage = (Stage) viewEditMenuBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation redirects the user to the List Flashcards page.
     * Called from the View/Edit MenuButton.
     */
    @FXML
    protected void goToListFlashcardsOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-flashcards-view.fxml"));
            Stage stage = (Stage) viewEditMenuBtn.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation redirects the user to the Search Flashcards page.
     */
    @FXML
    public void goToSearchFlashcardsOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/search-flashcard-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 500));
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