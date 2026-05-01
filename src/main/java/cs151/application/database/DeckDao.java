package cs151.application.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DeckDao {

    private static final Logger logger = LoggerFactory.getLogger(DeckDao.class);
    private final DatabaseController db = DatabaseController.getInstance();

    /**
     * Represents a single row from Deck_table.
     * Record fields are accessed via id(), deckName(), description().
     */
    public record Deck(int id, String deckName, String description, String creationDate, String lastVisited) {}

    /**
     * Creates a DeckDao instance.
     * DatabaseController must be initialized before creating a DeckDao.
     * No connection is held — each method opens and closes its own connection.
     */
    public DeckDao() {}

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
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
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
        String sql = "SELECT id, deck_name, description, creation_date, last_visited FROM Deck_table ORDER BY deck_name";
        List<Deck> decks = new ArrayList<>();
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {   // ← now properly inside try-with-resources
            while (rs.next()) {
                decks.add(new Deck(
                        rs.getInt("id"),
                        rs.getString("deck_name"),
                        rs.getString("description"),
                        rs.getString("creation_date"),
                        rs.getString("last_visited")
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
        String sql = "SELECT id, deck_name, description, creation_date, last_visited FROM Deck_table WHERE deck_name = ?";
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Deck(
                            rs.getInt("id"),
                            rs.getString("deck_name"),
                            rs.getString("description"),
                            rs.getString("creation_date"),
                            rs.getString("last_visited")
                    );
                }
            }
        }
        return null;
    }

    /**
     * Finds and returns a single deck by its primary key.
     * Returns null if no deck with that id exists.
     */
    public Deck getDeckById(int deckId) throws SQLException {
        String sql = "SELECT id, deck_name, description, creation_date, last_visited FROM Deck_table WHERE id = ?";
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return new Deck(
                            rs.getInt("id"),
                            rs.getString("deck_name"),
                            rs.getString("description"),
                            rs.getString("creation_date"),
                            rs.getString("last_visited")
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
     * Manually syncs deck_name in Flashcard_table since the foreign key
     * is on deck_id (not deck_name), so ON UPDATE CASCADE does not cover it.
     * Both updates run in a single transaction — if either fails, both are rolled back.
     */
    public void updateDeck(String oldDeckName, String newDeckName, String newDescription) throws SQLException {
        String updateDeckSql = "UPDATE Deck_table SET deck_name = ?, description = ? WHERE deck_name = ?";
        String syncFlashcardsSql = "UPDATE Flashcard_table SET deck_name = ? WHERE deck_name = ?";

        try (Connection conn = db.openConnection()) {
            conn.setAutoCommit(false);
            try {
                try (PreparedStatement stmt = conn.prepareStatement(updateDeckSql)) {
                    stmt.setString(1, newDeckName);
                    stmt.setString(2, newDescription);
                    stmt.setString(3, oldDeckName);
                    int rows = stmt.executeUpdate();
                    if (rows == 0) {
                        throw new SQLException("No deck found with name: " + oldDeckName);
                    }
                    logger.info("Updated deck: {} → {}", oldDeckName, newDeckName);
                }

                try (PreparedStatement stmt = conn.prepareStatement(syncFlashcardsSql)) {
                    stmt.setString(1, newDeckName);
                    stmt.setString(2, oldDeckName);
                    stmt.executeUpdate();
                    logger.info("Synced deck_name in Flashcard_table: {} → {}", oldDeckName, newDeckName);
                }

                conn.commit();
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
        }
    }

    /**
     * Stamps last_visited with the current database time.
     * Call this every time the user navigates into a deck to view its flashcards.
     */
    public void updateLastVisited(int deckId) throws SQLException {
        String sql = "UPDATE Deck_table SET last_visited = datetime('now') WHERE id = ?";
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            stmt.executeUpdate();
            logger.info("Updated last_visited for deck id: {}", deckId);
        }
    }
    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    /**
     * Deletes a deck by name.
     * Because of ON DELETE CASCADE, all flashcards belonging to this deck
     * are automatically deleted from Flashcard_table as well.
     * Throws SQLException if no deck with the given name exists.
     */
    public void deleteDeck(String deckName) throws SQLException {
        String sql = "DELETE FROM Deck_table WHERE deck_name = ?";
        try (Connection conn = db.openConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            int rows = stmt.executeUpdate();
            if (rows == 0) {
                throw new SQLException("No deck found with name: " + deckName);
            }
            logger.info("Deleted deck: {}", deckName);
        }
    }
}
