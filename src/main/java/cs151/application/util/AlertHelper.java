package cs151.application.util;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

/**
 * Utility class for displaying reusable alert dialogs across all controllers.
 * Centralizes error and confirmation dialog logic in one place.
 */
public class AlertHelper {

    /**
     * Helper method that displays a popup dialog to the user.
     *
     * @param type    the type of alert (e.g., ERROR, WARNING, INFORMATION)
     * @param title   the title text of the alert window
     * @param message the body message displayed in the alert
     */
    public static void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

}