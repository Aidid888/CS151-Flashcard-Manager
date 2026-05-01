package cs151.application;

import cs151.application.util.AlertHelper;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import cs151.application.database.DatabaseController;

import java.io.IOException;

public class Main extends Application {
    /**
     * Loads and displays the home view as the initial application scene.
     *
     * @param stage the primary stage provided by JavaFX
     * @throws Exception if the FXML file cannot be loaded
     */
    @Override
    public void start(Stage stage) throws IOException {
        DatabaseController.initialize(System.getProperty("user.dir") + "/flashcards.db");

        FXMLLoader fxmlLoader = new FXMLLoader(Main.class.getResource("view/home-view.fxml"));
        Scene scene = new Scene(fxmlLoader.load(), 700, 600);
        stage.setTitle("Flashcard Manager");
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        DatabaseController.getInstance().close();
    }

    /**
     * Launches the application and registers a global uncaught exception handler.
     * The handler catches any exception not handled by a controller and
     * displays an error alert instead of crashing silently.
     *
     * @param args command-line arguments (unused)
     */
    public static void main(String[] args) {
        Thread.setDefaultUncaughtExceptionHandler((thread, throwable) -> {
            throwable.printStackTrace();
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Unexpected Error",
                    "An unexpected error occurred:\n" + throwable.getMessage());
        });
        launch();
    }
}