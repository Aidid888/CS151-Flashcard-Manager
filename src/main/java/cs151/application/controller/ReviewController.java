package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import cs151.application.database.FlashcardDao;
import cs151.application.util.AlertHelper;
import cs151.application.util.DateTimeUtil;
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
import java.util.List;
import java.util.stream.Collectors;

/**
 * Controller for the Review page.
 * Handles deck search and selection for reviewing flashcards.
 * Users can search for decks and click on a deck to start reviewing its flashcards.
 */
public class ReviewController {

    @FXML private TextField searchField;
    @FXML private TableView<Deck> deckTable;
    @FXML private TableColumn<Deck, String> nameColumn;
    @FXML private TableColumn<Deck, String> descColumn;
    @FXML private TableColumn<Deck, String> creationDateColumn;
    @FXML private TableColumn<Deck, String> lastVisitedColumn;
    @FXML private TableColumn<Deck, Integer> totalFlashcardsColumn;

    private final DeckDao deckDao = new DeckDao();
    private final FlashcardDao flashcardDao = new FlashcardDao();
    private final ObservableList<Deck> deckList = FXCollections.observableArrayList();
    private List<Deck> allDecks;
    private final java.util.Map<Integer, Integer> flashcardCounts = new java.util.HashMap<>();

    /**
     * Initializes the TableView and search functionality when the page loads.
     */
    @FXML
    public void initialize() {
        deckTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_ALL_COLUMNS);

        // Configure table columns
        nameColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(cell.getValue().deckName()));

        descColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().description() == null ? "" : cell.getValue().description()));

        creationDateColumn.setCellValueFactory(cell ->
                new SimpleStringProperty(
                        cell.getValue().creationDate() == null
                                ? ""
                                : DateTimeUtil.utcToLocal(cell.getValue().creationDate())
                ));

        lastVisitedColumn.setCellValueFactory(cell -> {
            String lastVisited = cell.getValue().lastVisited();
            if (lastVisited == null) {
                return new SimpleStringProperty("Never");
            }
            return new SimpleStringProperty(DateTimeUtil.utcToLocal(lastVisited));
        });

        totalFlashcardsColumn.setCellValueFactory(cell ->
                new javafx.beans.property.SimpleObjectProperty<>(
                        flashcardCounts.getOrDefault(cell.getValue().id(), 0)
                )
        );

        // Double-click to review deck
        deckTable.setRowFactory(tv -> {
            TableRow<Deck> row = new TableRow<>();
            row.setOnMouseClicked(event -> {
                if (event.getClickCount() == 2 && !row.isEmpty()) {
                    navigateToDeckReview(row.getItem());
                }
            });
            return row;
        });

        deckTable.setItems(deckList);
        loadDecks();

        // Live search on every keystroke
        searchField.textProperty().addListener(
                (obs, oldVal, newVal) -> filterDecks(newVal));
    }

    /**
     * Loads all decks from the database and populates the TableView.
     * Also loads flashcard counts for each deck.
     */
    private void loadDecks() {
        try {
            allDecks = deckDao.getAllDecks();
            flashcardCounts.clear();
            for (Deck d : allDecks) {
                flashcardCounts.put(d.id(), flashcardDao.getFlashcardCountByDeckId(d.id()));
            }
            deckList.setAll(allDecks);
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not load decks:\n" + e.getMessage());
        }
    }

    /**
     * Filters the deck list based on the search keyword.
     * Searches in deck name and description (case-insensitive).
     *
     * @param keyword the search term from the search field
     */
    private void filterDecks(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            deckList.setAll(allDecks);
            return;
        }

        String lowerKeyword = keyword.toLowerCase();
        List<Deck> filtered = allDecks.stream()
                .filter(deck -> {
                    boolean matchesName = deck.deckName().toLowerCase().contains(lowerKeyword);
                    boolean matchesDesc = deck.description() != null &&
                            deck.description().toLowerCase().contains(lowerKeyword);
                    return matchesName || matchesDesc;
                })
                .collect(Collectors.toList());
        deckList.setAll(filtered);
    }

    /**
     * Navigates to the Deck Review page for the selected deck.
     * Updates the deck's last visited timestamp before navigating.
     *
     * @param deck the selected deck to review
     */
    private void navigateToDeckReview(Deck deck) {
        try {
            // Update last visited timestamp
            deckDao.updateLastVisited(deck.id());

            FXMLLoader loader = new FXMLLoader(
                    Main.class.getResource("view/deck-review-view.fxml"));
            Stage stage = (Stage) deckTable.getScene().getWindow();
            stage.setScene(new Scene(loader.load(), 700, 600));

            DeckReviewController controller = loader.getController();
            controller.setDeck(deck);

            stage.show();
        } catch (IOException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not open Deck Review view:\n" + e.getMessage());
        } catch (SQLException e) {
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Database Error",
                    "Could not update last visited:\n" + e.getMessage());
        }
    }

    /**
     * Navigates back to the home page.
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
            AlertHelper.showAlert(Alert.AlertType.ERROR, "Navigation Error",
                    "Could not return home:\n" + e.getMessage());
        }
    }
}