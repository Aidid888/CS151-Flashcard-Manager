package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.sql.SQLException;
import java.util.List;

public class SearchFlashcardController {

    // ----------------------------------------------------------------
    // FXML-injected controls  (each fx:id in the FXML maps to one field)
    // ----------------------------------------------------------------

    /** Text field the user types into; every keystroke triggers a re-query. */
    @FXML private TextField searchField;

    /** Table that renders matching flashcard rows. */
    @FXML private TableView<Flashcard> flashcardTable;

    @FXML private TableColumn<Flashcard, String> deckNameColumn;
    @FXML private TableColumn<Flashcard, String> frontTextColumn;   // TextArea → first line only
    @FXML private TableColumn<Flashcard, String> backTextColumn;    // TextArea → first line only
    @FXML private TableColumn<Flashcard, String> statusColumn;
    @FXML private TableColumn<Flashcard, String> creationDateColumn;
    @FXML private TableColumn<Flashcard, String> lastViewedColumn;

    /** Button-per-row column; no backing Flashcard property — built via cell factory. */
    @FXML private TableColumn<Flashcard, Void> deleteColumn;

    // ----------------------------------------------------------------
    // Instance variables
    // ----------------------------------------------------------------

    /**
     * DAO used for all flashcard DB operations.
     * Created once in initialize() and reused for every search and delete.
     */
    private FlashcardDao flashcardDao;

    /**
     * Mutable backing list for the TableView.
     * Held as a field so deleteRow() can remove a single item in O(n)
     * without issuing a new DB query after every deletion.
     */
    private ObservableList<Flashcard> tableData;

    // ----------------------------------------------------------------
    // Initialization (called automatically by JavaFX after FXML injection)
    // ----------------------------------------------------------------

    /**
     * Sets up the controller after all @FXML fields are injected:
     *  1. Creates the DAO and the observable backing list.
     *  2. Binds data columns to Flashcard record accessors via PropertyValueFactory.
     *  3. Applies a first-line-only cell factory to the two TextArea-backed columns.
     *  4. Wires the delete column with a button cell factory.
     *  5. Loads all flashcards into the table (empty keyword = wildcard).
     *  6. Attaches a ChangeListener so the table filters live as the user types.
     */
    @FXML
    public void initialize() {
        flashcardDao = new FlashcardDao();
        tableData    = FXCollections.observableArrayList();

        // Simple string columns — property names match Flashcard record accessors
        deckNameColumn    .setCellValueFactory(new PropertyValueFactory<>("deckName"));
        statusColumn      .setCellValueFactory(new PropertyValueFactory<>("status"));
        creationDateColumn.setCellValueFactory(new PropertyValueFactory<>("creationDate"));
        lastViewedColumn  .setCellValueFactory(new PropertyValueFactory<>("lastViewed"));

        // TextArea columns: bind value then override rendering to show first line only
        frontTextColumn.setCellValueFactory(new PropertyValueFactory<>("frontText"));
        frontTextColumn.setCellFactory(col -> firstLineCell());

        backTextColumn.setCellValueFactory(new PropertyValueFactory<>("backText"));
        backTextColumn.setCellFactory(col -> firstLineCell());

        // Delete column has no data property — entirely driven by its cell factory
        deleteColumn.setCellFactory(buildDeleteCellFactory());

        flashcardTable.setItems(tableData);

        loadResults("");   // show all cards on first open

        // Re-query on every keystroke; newVal is the current field text
        searchField.textProperty().addListener(
                (obs, oldVal, newVal) -> loadResults(newVal));
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Queries the database with the given keyword and refreshes the table.
     * A blank keyword returns every flashcard (the DAO uses a wildcard pattern).
     * On SQLException the table is cleared rather than crashing the UI.
     *
     * @param keyword raw text from searchField; trimmed before being sent to DAO
     */
    private void loadResults(String keyword) {
        try {
            List<Flashcard> results = flashcardDao.searchFlashcards(keyword.trim());
            tableData.setAll(results);
        } catch (SQLException e) {
            e.printStackTrace();
            tableData.clear();
        }
    }

    /**
     * Returns a TableCell that renders only the first line of a multi-line string.
     * Satisfies the requirement: "If a field is TextArea, only the first line
     * of the data is shown."
     * split("\\R", 2) handles \n, \r\n, and \r uniformly.
     */
    private TableCell<Flashcard, String> firstLineCell() {
        return new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty || value == null) {
                    setText(null);
                } else {
                    setText(value.split("\\R", 2)[0]);
                }
            }
        };
    }

    /**
     * Builds a Callback that produces a Delete button inside every non-empty row.
     *
     * On button click:
     *  1. A confirmation dialog is displayed with a preview of the card's front text.
     *  2. If confirmed, the flashcard is deleted from the DB via the DAO.
     *  3. The row is removed directly from tableData — no full re-query needed.
     */
    private Callback<TableColumn<Flashcard, Void>, TableCell<Flashcard, Void>>
    buildDeleteCellFactory() {

        return col -> new TableCell<>() {

            /** Reused button instance per cell — created once, not on every render. */
            private final Button deleteBtn = new Button("Delete");

            {
                // Style the button and attach the click handler in the initializer block
                deleteBtn.setStyle(
                        "-fx-background-color: #c0392b; -fx-text-fill: white;");
                deleteBtn.setOnAction(e -> {
                    Flashcard card = getTableView().getItems().get(getIndex());
                    if (confirmDeletion(card)) {
                        try {
                            flashcardDao.deleteFlashcard(card.id());
                            tableData.remove(card); // instant UI update, no re-query
                        } catch (SQLException ex) {
                            ex.printStackTrace();
                            showError("Delete failed: " + ex.getMessage());
                        }
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                // Show button only in populated rows; null-out empty rows
                setGraphic(empty ? null : deleteBtn);
            }
        };
    }

    /**
     * Displays a confirmation dialog before permanently deleting a flashcard.
     * Shows only the first line of frontText so the dialog stays compact.
     *
     * @param card the flashcard the user clicked Delete on
     * @return true if the user pressed OK; false if they cancelled
     */
    private boolean confirmDeletion(Flashcard card) {
        String preview = card.frontText().split("\\R", 2)[0];
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirm Deletion");
        alert.setHeaderText("Delete this flashcard?");
        alert.setContentText("\"" + preview + "\" will be permanently removed.");
        return alert.showAndWait()
                .filter(btn -> btn == ButtonType.OK)
                .isPresent();
    }

    /**
     * Shows a generic error alert. Used when a DB operation fails unexpectedly
     * so the user gets feedback instead of a silent failure.
     *
     * @param message the error detail to display
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText("An error occurred");
        alert.setContentText(message);
        alert.showAndWait();
    }

    // ----------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------

    /**
     * Navigates back to the home view when the Back button is pressed.
     * Loads home-view.fxml and replaces the current stage scene.
     */
    @FXML
    private void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Scene scene = new Scene(loader.load(), 600, 500);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}