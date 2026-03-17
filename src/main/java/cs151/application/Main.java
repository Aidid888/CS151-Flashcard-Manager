package cs151.application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import cs151.application.database.DatabaseController;

import java.io.IOException;

public class Main extends Application {
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseController.initialize(System.getProperty("user.dir") + "/flashcards.db");

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/home-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        stage.setTitle("Flashcard Manager");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseController.getInstance().close();
    }


    public static void main(String[] args) {
        launch();
    }
}