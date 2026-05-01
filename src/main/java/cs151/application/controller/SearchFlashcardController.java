package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import cs151.application.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Callback;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;

import cs151.application.util.DateTimeUtil;

/**
 * Controller for the Search Flashcard view.
 * Handles live search filtering, displaying results in a TableView,
 * and deleting individual flashcards.
 */
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

    /** Button-per-row column; built entirely via cell factory. */
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
     * Held as a field so deleteRow() can remove a single item.
     */
    private ObservableList<Flashcard> tableData;

    // ----------------------------------------------------------------
    // Initialization (called automatically by JavaFX after FXML injection)
    // ----------------------------------------------------------------

    /**
     * Initializes the TableView columns, delete button, and search listener on load.
     * Defaults to showing all flashcards until the user types in the search field.
     */
    @FXML
    public void initialize() {
        flashcardDao = new FlashcardDao();
        tableData    = FXCollections.observableArrayList();

        // Simple string columns — property names match Flashcard record accessors
        deckNameColumn    .setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().deckName()));
        statusColumn      .setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().status()));
        creationDateColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        DateTimeUtil.utcToLocal(cell.getValue().creationDate())
                ));
        lastViewedColumn.setCellValueFactory(cell -> {
            String lastViewed = cell.getValue().lastViewed();
            if (lastViewed == null) {
                return new javafx.beans.property.SimpleStringProperty("Never");
            }
            return new javafx.beans.property.SimpleStringProperty(DateTimeUtil.utcToLocal(lastViewed));
        });

        // TextArea columns: bind value then override rendering to show first line only
        frontTextColumn.setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().frontText()));
        backTextColumn .setCellValueFactory(cell -> new javafx.beans.property.SimpleStringProperty(cell.getValue().backText()));

        // Delete column has no data property — entirely driven by its cell factory
        deleteColumn.setCellFactory(buildDeleteCellFactory());

        // Double-click to edit
        flashcardTable.setRowFactory(tv -> {
            TableRow<Flashcard> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openEditPopup(row.getItem());
                }
            });
            return row;
        });

        flashcardTable.setItems(tableData);

        loadResults("");   // show all cards on first open

        // Re-query on every keystroke
        searchField.textProperty().addListener(
                (obs, oldVal, newVal) -> loadResults(newVal));
    }

    // ----------------------------------------------------------------
    // Private helpers
    // ----------------------------------------------------------------

    /**
     * Queries the database with the given keyword and refreshes the table.
     * A blank keyword returns all flashcards. Clears the table on failure.
     *
     * @param keyword the search term from the search field
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
     * Returns a TableCell that displays only the first line of a multi-line string.
     * Used for front and back text columns to keep rows compact.
     * Method split("\\R", 2) handles \n, \r\n, and \r uniformly.
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
     * Builds a cell factory that renders a Delete button on each non-empty row.
     * Prompts for confirmation before deleting the flashcard from the database
     * and removing it from the table.
     *
     * @return a Callback that produces delete button cells for the delete column
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
                            AlertHelper.showAlert(Alert.AlertType.ERROR, "Delete failed: ", "Could not delete flashcard:\n" + ex.getMessage());
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
     * @return true if the user confirmed; false if canceled
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
     * Opens a modal edit popup for the given flashcard.
     * Refreshes the table after the popup closes if changes were saved.
     *
     * @param card the flashcard to edit
     */
    private void openEditPopup(Flashcard card) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/edit-flashcard-view.fxml"));
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL);
            popupStage.setTitle("Edit Flashcard");
            popupStage.setScene(new Scene(loader.load(), 450, 600));

            EditFlashcardController editController = loader.getController();
            editController.setFlashcard(card);
            editController.setOnSaved(() -> {
                // Refresh the table with current search term
                loadResults(searchField.getText());
            });

            popupStage.showAndWait();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open edit window:\n" + e.getMessage());
        }
    }


    // ----------------------------------------------------------------
    // Navigation
    // ----------------------------------------------------------------

    /**
     * Navigates the user back to the home view.
     *
     * @param event the action event triggered by the back button
     */
    @FXML
    private void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Scene scene = new Scene(loader.load(), 700, 600);
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(scene);
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}