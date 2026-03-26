package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
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

public class ListDeckController {

    @FXML
    private TableView<Deck> deckTable;
    @FXML
    private TableColumn<Deck, String> nameColumn;
    @FXML
    private TableColumn<Deck, String> descColumn;
    @FXML
    private TableColumn<Deck, Void> actionsColumn;
    @FXML
    private TableColumn<Deck, Integer> totalFlashcardsInDeck;

    private final DeckDao deckDao = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();
    private final ObservableList<Deck> deckList = FXCollections.observableArrayList();

    /**
     * Initializes TableView and list of Decks when the page loads.
     */
    @FXML
    public void initialize() {
        deckTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        nameColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().deckName()
                )
        );

        descColumn.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleStringProperty(
                        cellData.getValue().description() == null
                                ? ""
                                : cellData.getValue().description()
                )
        );

        totalFlashcardsInDeck.setCellValueFactory(cell -> {
            try {
                int count = flashcardDao.getFlashcardCountByDeckId(cell.getValue().id());
                return new javafx.beans.property.SimpleObjectProperty<>(count);
            } catch (SQLException e) {
                return new javafx.beans.property.SimpleObjectProperty<>(0);
            }
        });

        actionsColumn.setCellFactory(col -> new ActionCell());

        deckTable.setItems(deckList);
        loadDecks();
    }

    /**
     * Loads list of Decks from the database.
     */
    private void loadDecks() {
        try {
            List<Deck> deckList = deckDao.getAllDecks();

            ObservableList<Deck> observableList =
                    FXCollections.observableArrayList(deckList);

            deckTable.setItems(observableList);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * The operation returns the user back to homepage.
     */
    @FXML
    private void goBackHome(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/home-view.fxml"));

            Scene scene = new Scene(loader.load(), 600, 500);

            Stage stage = (Stage)((Node)event.getSource())
                    .getScene().getWindow();

            stage.setScene(scene);
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ---------------------------------------------------------------
    // Action handlers called in the Action trigger button.
    // ---------------------------------------------------------------

    private void handleSeeAllFlashcards(Deck deck) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/list-flashcards-view.fxml"));

            Stage stage = (Stage) deckTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load()));

            Object controller = loader.getController();
            if (controller instanceof ListFlashcardController lfc) {
                lfc.setDeck(deck);
            }

            stage.show();
        } catch (IOException e) {
            showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open Add Flashcard view:\n" + e.getMessage());
        }
    }

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
                new Label("Name:"),        nameField,
                new Label("Description:"), descField);
        content.setPadding(new javafx.geometry.Insets(10));
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        Optional<ButtonType> result = dialog.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) return;

        String newName = nameField.getText().trim();
        if (newName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation", "Deck name cannot be empty.");
            return;
        }

        try {
            deckDao.updateDeck(deck.deckName(), newName, descField.getText().trim());
            loadDecks();
        } catch (SQLException e) {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update deck:\n" + e.getMessage());
        }
    }

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
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not delete deck:\n" + e.getMessage());
        }
    }

    /**
     * Helper method that displays a popup dialog to the user.
     * @param type
     * @param title
     * @param message
     */
    private void showAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    // ---------------------------------------------------------------
    // Inner class: ActionCell
    // ---------------------------------------------------------------

    /**
     * Renders a trigger button for actions on every non-empty row.
     * Kept as a private inner class so it can call the outer controller's handler methods directly — no extra coupling needed.
     */
    private class ActionCell extends TableCell<Deck, Void> {

        private final Button menuBtn = new Button("⋮");
        private final ContextMenu menu = new ContextMenu();

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

        @Override
        protected void updateItem(Void item, boolean empty) {
            super.updateItem(item, empty);
            setGraphic(empty || getTableRow().getItem() == null ? null : menuBtn);
        }
    }
}