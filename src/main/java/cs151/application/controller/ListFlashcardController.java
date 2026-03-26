package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.database.FlashcardDao.Flashcard;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ListFlashcardController {

    @FXML private ComboBox<Deck>               deckComboBox;
    @FXML private TableView<Flashcard>         flashcardTable;
    @FXML private TableColumn<Flashcard, String> deckNameColumn;
    @FXML private TableColumn<Flashcard, String> frontTextColumn;
    @FXML private TableColumn<Flashcard, String> backTextColumn;
    @FXML private TableColumn<Flashcard, String> statusColumn;
    @FXML private TableColumn<Flashcard, String> creationDateColumn;
    @FXML private TableColumn<Flashcard, String> lastViewedColumn;

    private final DeckDao      deckDao      = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();
    private final ObservableList<Flashcard> flashcardList =
            FXCollections.observableArrayList();

    /**
     * Initialization
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

        flashcardTable.setItems(flashcardList);

        // Show deck names in the ComboBox
        deckComboBox.setCellFactory(lv -> new DeckCell());
        deckComboBox.setButtonCell(new DeckCell());

        loadDeckComboBox();

        // Default: show all flashcards until a deck is selected
        loadAllFlashcards();
    }

    /**
     * Called by ListDeckController when navigating from a specific deck row.
     * Pre-selects that deck in the ComboBox and filters the table automatically.
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
     * Event handler — ComboBox selection
     */
    @FXML
    private void onDeckSelected() {
        Deck selected = deckComboBox.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        loadFlashcardsForDeck(selected);
    }

    /**
     * Loads data from database into combobox.
     */
    private void loadDeckComboBox() {
        try {
            deckComboBox.setItems(
                    FXCollections.observableArrayList(deckDao.getAllDecks()));
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load decks:\n" + e.getMessage());
        }
    }

    private void loadFlashcardsForDeck(Deck deck) {
        try {
            List<Flashcard> cards =
                    flashcardDao.getFlashcardsByDeckId(deck.id());
            cards.sort((a, b) -> b.creationDate().compareTo(a.creationDate()));
            flashcardList.setAll(cards);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load flashcards:\n" + e.getMessage());
        }
    }

    private void loadAllFlashcards() {
        try {
            List<Flashcard> all = new ArrayList<>();
            for (Deck deck : deckDao.getAllDecks()) {
                all.addAll(flashcardDao.getFlashcardsByDeckId(deck.id()));
            }
            all.sort((a, b) -> b.creationDate().compareTo(a.creationDate()));
            flashcardList.setAll(all);
        } catch (SQLException e) {
            showAlert("Database Error", "Could not load flashcards:\n" + e.getMessage());
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
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 600, 500));
            stage.show();
        } catch (IOException e) {
            showAlert("Navigation Error", "Could not return home:\n" + e.getMessage());
        }
    }

    /**
     * Helper method that displays a popup dialog to the user.
     * @param title
     * @param message
     */
    private void showAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR, message, ButtonType.OK);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.showAndWait();
    }

    /**
     * Renders Deck records in the ComboBox by name instead of the default Object.toString() output.
     */
    private static class DeckCell extends ListCell<Deck> {
        @Override
        protected void updateItem(Deck deck, boolean empty) {
            super.updateItem(deck, empty);
            setText(empty || deck == null ? null : deck.deckName());
        }
    }
}