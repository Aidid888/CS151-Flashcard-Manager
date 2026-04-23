package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import cs151.application.util.AlertHelper;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * Controller for the Flashcard List view.
 * Handles displaying, filtering, editing, and deleting flashcards.
 */
public class ListFlashcardController {

    @FXML private ComboBox<Deck>               deckComboBox;
    @FXML private TableView<Flashcard>         flashcardTable;
    @FXML private TableColumn<Flashcard, String> deckNameColumn;
    @FXML private TableColumn<Flashcard, String> frontTextColumn;
    @FXML private TableColumn<Flashcard, String> backTextColumn;
    @FXML private TableColumn<Flashcard, String> statusColumn;
    @FXML private TableColumn<Flashcard, String> creationDateColumn;
    @FXML private TableColumn<Flashcard, String> lastViewedColumn;
    @FXML private TableColumn<Flashcard, Void> deleteColumn;

    private final DeckDao      deckDao      = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();
    private final ObservableList<Flashcard> flashcardList =
            FXCollections.observableArrayList();

    /**
     * Initializes the TableView columns, delete button, row double-click, and
     * deck ComboBox on load. Shows all flashcards until a deck is selected.
     */
    @FXML
    public void initialize() {
        flashcardTable.setColumnResizePolicy(
                TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        deckNameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().deckName()));
        frontTextColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().frontText()));
        backTextColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().backText()));
        statusColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().status()));
        creationDateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().creationDate()));
        lastViewedColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().lastViewed() == null
                                ? "Never"
                                : cell.getValue().lastViewed()));
        deleteColumn.setCellFactory(col -> new TableCell<>() {
            private final Button deleteBtn = new Button("Delete");
            {
                deleteBtn.setStyle("-fx-background-color: #c0392b; -fx-text-fill: white;");
                deleteBtn.setOnAction(e -> {
                    Flashcard card = getTableView().getItems().get(getIndex());
                    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
                    confirm.setTitle("Confirm Deletion");
                    confirm.setHeaderText("Delete this flashcard?");
                    confirm.setContentText("\"" + card.frontText().split("\\R", 2)[0] + "\" will be permanently removed.");
                    confirm.showAndWait().filter(btn -> btn == ButtonType.OK).ifPresent(btn -> {
                        try {
                            flashcardDao.deleteFlashcard(card.id());
                            flashcardList.remove(card);
                        } catch (SQLException ex) {
                            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete flashcard:\n" + ex.getMessage());
                        }
                    });
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : deleteBtn);
            }
        });

        flashcardTable.setItems(flashcardList);
        flashcardTable.setRowFactory(tv -> {
            TableRow<Flashcard> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    openEditPopup(row.getItem());
                }
            });
            return row;
        });

        // Show deck names in the ComboBox
        deckComboBox.setCellFactory(lv -> new DeckCell());
        deckComboBox.setButtonCell(new DeckCell());

        loadDeckComboBox();

        // Default: show all flashcards until a deck is selected
        loadAllFlashcards();
    }

    /**
     * Pre-selects the given deck in the ComboBox and filters the table to match.
     * Called by ListDeckController when navigating from a specific deck row.
     *
     * @param deck the deck to pre-select
     */
    public void setDeck(Deck deck) {
        // Match by id in case the list contains a freshly fetched copy
        deckComboBox.getItems().stream()
                .filter(d -> d.id() == deck.id())
                .findFirst()
                .ifPresent(d -> {
                    deckComboBox.getSelectionModel().select(d);
                    loadFlashcardsForDeck(d);
                });
    }

    /**
     * Filters the TableView to show only flashcards from the selected deck.
     * Triggered when the user picks a deck from the ComboBox.
     */
    @FXML
    private void onDeckSelected() {
        // Event handler — ComboBox selection.
        Deck selected = deckComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        loadFlashcardsForDeck(selected);
    }

    /**
     * Loads all decks from the database into the deck ComboBox.
     * Shows an error alert if the query fails.
     */
    private void loadDeckComboBox() {
        try {
            deckComboBox.setItems(
                    FXCollections.observableArrayList(deckDao.getAllDecks()));
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load decks:\n" + e.getMessage());
        }
    }

    /**
     * Loads flashcards for the given deck into the TableView,
     * sorted by creation date descending.
     *
     * @param deck the deck whose flashcards will be loaded
     */

    private void loadFlashcardsForDeck(Deck deck) {
        try {
            List<Flashcard> cards =
                    flashcardDao.getFlashcardsByDeckId(deck.id());
            cards.sort((a, b) -> b.creationDate().compareTo(a.creationDate()));
            flashcardList.setAll(cards);
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load flashcards:\n" + e.getMessage());
        }
    }

    /**
     * Loads all flashcards into the TableView,
     * sorted by creation date descending.
     */
    private void loadAllFlashcards() {
        try {
            List<Flashcard> all = flashcardDao.searchFlashcards("");
            all.sort((a, b) -> b.creationDate().compareTo(a.creationDate()));
            flashcardList.setAll(all);
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error", "Could not load flashcards:\n" + e.getMessage());
        }
    }

    /**
     * The operation returns the user back to homepage.
     *
     * @param event the action event triggered by the back button
     */
    @FXML
    private void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not return home:\n" + e.getMessage());
        }
    }

    /**
     * Opens a modal edit popup for the given flashcard.
     * Refreshes the TableView after the popup closes if changes were saved.
     *
     * @param card the flashcard to edit
     */
    private void openEditPopup(Flashcard card) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/edit-flashcard-view.fxml"));
            Stage popupStage = new Stage();
            popupStage.initModality(Modality.APPLICATION_MODAL); // blocks parent window
            popupStage.setTitle("Edit Flashcard");
            popupStage.setScene(new Scene(loader.load(), 450, 600));

            EditFlashcardController editController = loader.getController();
            editController.setFlashcard(card);
            editController.setOnSaved(() -> {
                // Refresh the table after saving
                Deck selected = (Deck) deckComboBox.getSelectionModel().getSelectedItem();
                if (selected != null) loadFlashcardsForDeck(selected);
                else loadAllFlashcards();
            });

            popupStage.showAndWait();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error", "Could not open edit window:\n" + e.getMessage());
        }
    }


    /**
     * Custom ListCell that renders a Deck by its name in the ComboBox instead of the default Object.toString() output.
     */
    private static class DeckCell extends ListCell<Deck> {
        /**
         * Sets the cell text to the deck's name, or clears it if the cell is empty.
         *
         * @param deck  the Deck to display
         * @param empty true if the cell has no data, false otherwise
         */
        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            setText(empty || deck == null ? null : deck.deckName());
        }
    }
}