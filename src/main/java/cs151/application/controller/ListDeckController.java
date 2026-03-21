package cs151.application.controller;

import cs151.application.Main;
import cs151.application.database.DeckDao;
import cs151.application.database.DeckDao.Deck;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.Node;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.event.ActionEvent;
import javafx.scene.control.cell.PropertyValueFactory;

import java.util.List;

public class ListDeckController {

    @FXML
    private TableView<Deck> deckTable;

    @FXML
    private TableColumn<Deck, String> nameColumn;

    @FXML
    private TableColumn<Deck, String> descColumn;

    private final DeckDao deckDao = new DeckDao();

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
        loadDecks();
    }

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
}