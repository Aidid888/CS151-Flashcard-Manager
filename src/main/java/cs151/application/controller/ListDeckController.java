package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.util.AlertHelper;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.event.ActionEvent;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Controller for List Deck page.
 * Manages displaying, updating, and deleting flashcard decks in a TableView.
 * Also handles navigation to the flashcard list for a selected deck.
 */
public class ListDeckController {

    @FXML private TableView<Deck>            deckTable;
    @FXML private TableColumn<Deck, String>  nameColumn;
    @FXML private TableColumn<Deck, String>  descColumn;
    @FXML private TableColumn<Deck, String>  creationDateColumn;
    @FXML private TableColumn<Deck, String>  lastVisitedColumn;
    @FXML private TableColumn<Deck, Integer> totalFlashcardsInDeck;
    @FXML private TableColumn<Deck, Void>    actionsColumn;

    private final DeckDao      deckDao      = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();
    private final ObservableList<Deck> deckList = FXCollections.observableArrayList();
    private final java.util.Map<Integer, Integer> flashcardCounts = new java.util.HashMap<>();

    /**
     * Initializes TableView and list of Decks when the page loads.
     */
    @FXML
    public void initialize() {
        deckTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);
        deckTable.setRowFactory(tv -> {
            TableRow<Deck> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    handleUpdateDeck(row.getItem());
                }
            });
            return row;
        });

        nameColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(cell.getValue().deckName()));

        descColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().description() == null ? "" : cell.getValue().description()));

        creationDateColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().creationDate() == null ? "" : cell.getValue().creationDate()));

        lastVisitedColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleStringProperty(
                        cell.getValue().lastVisited() == null ? "Never" : cell.getValue().lastVisited()));

        totalFlashcardsInDeck.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        flashcardCounts.getOrDefault(cell.getValue().id(), 0)
                )
        );

        actionsColumn.setCellFactory(col -> new ActionCell());

        deckTable.setItems(deckList);
        loadDecks();
    }

    /**
     * Loads list of Decks from the database and refreshes the Tableview.
     * Flashcard count for each deck is also displayed.
     */
    private void loadDecks() {
        try {
            List<Deck> decks = deckDao.getAllDecks();
            flashcardCounts.clear();
            for (Deck d : decks) {
                flashcardCounts.put(d.id(), flashcardDao.getFlashcardCountByDeckId(d.id()));
            }
            deckList.setAll(decks);
            deckTable.setItems(deckList);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation returns the user back to homepage.
     *
     * @param event the action event triggered by clicking the back button
     */
    @FXML
    private void goBackHomeOp(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));
            stage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // Action handlers called in the Action trigger button.
    // ---------------------------------------------------------------

    /**
     * Opens the flashcard list view for the given deck.
     * Updates the deck's last visited timestamp before navigating
     *
     * @param deck the selected deck corresponding flashcards will be displayed
     */
    private void handleSeeAllFlashcards(Deck deck) {
        try {
            // Stamp last visited when user navigates into a deck
            deckDao.updateLastVisited(deck.id());

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-flashcards-view.fxml"));
            Stage stage = (Stage) deckTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));

            Object controller = loader.getController();
            if (controller instanceof ListFlashcardController lfc) {
                lfc.setDeck(deck);
            }
            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open Flashcard view:\n" + e.getMessage());
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not update last visited:\n" + e.getMessage());
        }
    }

    /**
     * Opens a dialog for users to edit the name and description of a deck.
     * Saves changes to the database and refreshes the TableView on confirmation.
     *
     * @param deck the deck that will be updated
     */
    private void handleUpdateDeck(Deck deck) {
        // --- Name field ---
        TextField nameField = new TextField(deck.deckName());
        nameField.setPromptText("Deck name");

        // --- Description field ---
        TextField descField = new TextField(deck.description() == null ? "" : deck.description());
        descField.setPromptText("Description (optional)");

        // --- Dialog ---
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Update Deck");
        dialog.setHeaderText("Edit \"" + deck.deckName() + "\"");

        javafx.scene.layout.VBox content = new javafx.scene.layout.VBox(8,
                new Label("Name:"), nameField,
                new Label("Description:"), descField);
        content.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            AlertHelper.showAlert(Alert.AlertType.WARNING, "Validation", "Deck name cannot be empty.");
            return;
        }

        try {
            deckDao.updateDeck(deck.deckName(), newName, descField.getText().trim());
            loadDecks();
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not update deck:\n" + e.getMessage());
        }
    }

    /**
     * Prompts the user to confirm deletion of a deck, then removes it and all
     * associated flashcards from the database if confirmed.
     *
     * @param deck the deck that will be deleted
     */
    private void handleDeleteDeck(Deck deck) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION,
                "Delete \"" + deck.deckName() + "\" and all its flashcards?",
                ButtonType.YES, ButtonType.NO);
        confirm.setTitle("Confirm Delete");
        confirm.setHeaderText(null);

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.YES) return;

        try {
            deckDao.deleteDeck(deck.deckName());
            loadDecks();
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not delete deck:\n" + e.getMessage());
        }
    }

    // ---------------------------------------------------------------
    // Inner class: ActionCell
    // ---------------------------------------------------------------

    /**
     * Renders a trigger button for actions on every non-empty row.
     * Provides menu options to view flashcards, update, or delete the deck in that row.
     * Private inner class can directly access the outer controller's handler methods without additional coupling.
     */
    private class ActionCell extends TableCell<Deck, Void> {

        private final Button      menuBtn = new Button("⋮");
        private final ContextMenu menu    = new ContextMenu();

        ActionCell() {
            MenuItem addItem    = new MenuItem("See All Flashcards");
            MenuItem updateItem = new MenuItem("Update Deck");
            MenuItem deleteItem = new MenuItem("Delete Deck");

            addItem.setOnAction(e    -> handleSeeAllFlashcards(getTableRow().getItem()));
            updateItem.setOnAction(e -> handleUpdateDeck(getTableRow().getItem()));
            deleteItem.setOnAction(e -> handleDeleteDeck(getTableRow().getItem()));

            menu.getItems().addAll(addItem, updateItem, deleteItem);
            menuBtn.setOnAction(e ->
                    menu.show(menuBtn, javafx.geometry.Side.BOTTOM, 0, 0));
        }

        /**
         * Shows the menu button for non-empty rows and hides it for empty rows.
         *
         * @param item The new item for the cell.
         * @param empty true if the row contains no data, false otherwise.
         */
        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty || getTableRow().getItem() == null ? null : menuBtn);
        }
    }
}