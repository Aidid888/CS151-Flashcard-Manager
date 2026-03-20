package cs151.application.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeckDao {

    private static final Logger logger = LoggerFactory.getLogger(DeckDao.class);
    private final Connection conn;

    /**
     * Represents a single row from Deck_table.
     * Record fields are accessed via id(), deckName(), description().
     */
    public record Deck(int id, String deckName, String description) {}

    /**
     * Grabs the shared database connection from DatabaseController.
     * DatabaseController must be initialized before creating a DeckDao.
     */
    public DeckDao() {
        this.conn = DatabaseController.getInstance().getConnection();
    }

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------

    /**
     * Inserts a new deck into Deck_table.
     * deck_name must be unique — throws SQLException if a duplicate name is used.
     * description is optional and can be null.
     */
    public void insertDeck(String deckName, String description) throws SQLException {
        String sql = "INSERT INTO Deck_table (deck_name, description) VALUES (?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            stmt.setString(2, description);
            stmt.executeUpdate();
            logger.info("Inserted deck: {}", deckName);
        }
    }

    // ---------------------------------------------------------
    // READ
    // ---------------------------------------------------------

    /**
     * Returns all decks from Deck_table, sorted alphabetically by deck_name.
     * Returns an empty list if no decks exist.
     */
    public List<Deck> getAllDecks() throws SQLException {
        String sql = "SELECT id, deck_name, description FROM Deck_table ORDER BY deck_name";
        List<Deck> decks = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                decks.add(new Deck(
                        rs.getInt("id"),
                        rs.getString("deck_name"),
                        rs.getString("description")
                ));
            }
        }
        return decks;
    }

    /**
     * Finds and returns a single deck by its name.
     * Returns null if no deck with that name exists.
     */
    public Deck getDeckByName(String deckName) throws SQLException {
        String sql = "SELECT id, deck_name, description FROM Deck_table WHERE deck_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Deck(
                            rs.getInt("id"),
                            rs.getString("deck_name"),
                            rs.getString("description")
                    );
                }
            }
        }
        return null;
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    /**
     * Renames a deck and/or updates its description.
     * Because deck_name is a foreign key in Flashcard_table with ON UPDATE CASCADE,
     * all linked flashcards automatically update to the new name.
     */
    public void updateDeck(String oldDeckName, String newDeckName, String newDescription) throws SQLException {
        String sql = "UPDATE Deck_table SET deck_name = ?, description = ? WHERE deck_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newDeckName);
            stmt.setString(2, newDescription);
            stmt.setString(3, oldDeckName);
            stmt.executeUpdate();
            logger.info("Updated deck: {} → {}", oldDeckName, newDeckName);
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    /**
     * Deletes a deck by name.
     * Because of ON DELETE CASCADE, all flashcards belonging to this deck
     * are automatically deleted from Flashcard_table as well.
     */
    public void deleteDeck(String deckName) throws SQLException {
        String sql = "DELETE FROM Deck_table WHERE deck_name = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            stmt.executeUpdate();
            logger.info("Deleted deck: {}", deckName);
        }
    }
}
