package cs151.application.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FlashcardDao {

    private static final Logger logger = LoggerFactory.getLogger(FlashcardDao.class);
    private final Connection conn;

    /**
     * Represents a single row from Flashcard_table.
     * Record fields are accessed via id(), deckName(), frontText(), etc.
     */
    public record Flashcard(
            int id,
            String deckName,
            String frontText,
            String backText,
            String status,
            String creationDate,
            String lastViewed
    ) {}

    /**
     * Grabs the shared database connection from DatabaseController.
     * DatabaseController must be initialized before creating a FlashcardDao.
     */
    public FlashcardDao() {
        this.conn = DatabaseController.getInstance().getConnection();
    }

    // ---------------------------------------------------------
    // INSERT
    // ---------------------------------------------------------

    /**
     * Inserts a new flashcard into Flashcard_table under the given deck.
     * status defaults to 'new' and creation_date defaults to now — no need to pass them.
     * Throws SQLException if deckName doesn't exist in Deck_table (FK violation).
     */
    public void insertFlashcard(String deckName, String frontText, String backText) throws SQLException {
        String sql = "INSERT INTO Flashcard_table (deck_name, front_text, back_text) VALUES (?, ?, ?)";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            stmt.setString(2, frontText);
            stmt.setString(3, backText);
            stmt.executeUpdate();
            logger.info("Inserted flashcard into deck: {}", deckName);
        }
    }

    // ---------------------------------------------------------
    // READ
    // ---------------------------------------------------------

    /**
     * Returns all flashcards belonging to a specific deck, ordered by creation date.
     * Returns an empty list if the deck has no flashcards.
     */
    public List<Flashcard> getFlashcardsByDeck(String deckName) throws SQLException {
        String sql = "SELECT * FROM Flashcard_table WHERE deck_name = ? ORDER BY creation_date";
        List<Flashcard> cards = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, deckName);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    cards.add(mapRow(rs));
                }
            }
        }
        return cards;
    }

    /**
     * Returns every flashcard across all decks, grouped by deck and sorted by creation date.
     * Returns an empty list if no flashcards exist.
     */
    public List<Flashcard> getAllFlashcards() throws SQLException {
        String sql = "SELECT * FROM Flashcard_table ORDER BY deck_name, creation_date";
        List<Flashcard> cards = new ArrayList<>();
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                cards.add(mapRow(rs));
            }
        }
        return cards;
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    /**
     * Updates the status of a flashcard by its id.
     * Expected values: "new", "learning", "review", "mastered".
     */
    public void updateStatus(int id, String status) throws SQLException {
        String sql = "UPDATE Flashcard_table SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, id);
            stmt.executeUpdate();
            logger.info("Updated status for flashcard id {}: {}", id, status);
        }
    }

    /**
     * Sets last_viewed to the current timestamp for a given flashcard.
     * Call this whenever the user views or flips a card during a study session.
     */
    public void updateLastViewed(int id) throws SQLException {
        String sql = "UPDATE Flashcard_table SET last_viewed = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("Updated last_viewed for flashcard id {}", id);
        }
    }

    /**
     * Updates the front and/or back text of an existing flashcard by its id.
     * Use this when the user edits a card's content.
     */
    public void updateFlashcard(int id, String frontText, String backText) throws SQLException {
        String sql = "UPDATE Flashcard_table SET front_text = ?, back_text = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, frontText);
            stmt.setString(2, backText);
            stmt.setInt(3, id);
            stmt.executeUpdate();
            logger.info("Updated flashcard id {}", id);
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    /**
     * Deletes a single flashcard by its id.
     * Does not affect other flashcards in the same deck.
     */
    public void deleteFlashcard(int id) throws SQLException {
        String sql = "DELETE FROM Flashcard_table WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
            logger.info("Deleted flashcard id {}", id);
        }
    }

    // ---------------------------------------------------------
    // Helper
    // ---------------------------------------------------------

    /**
     * Maps a ResultSet row to a Flashcard record.
     * Centralizes column name access so it only needs to be updated in one place.
     */
    private Flashcard mapRow(ResultSet rs) throws SQLException {
        return new Flashcard(
                rs.getInt("id"),
                rs.getString("deck_name"),
                rs.getString("front_text"),
                rs.getString("back_text"),
                rs.getString("status"),
                rs.getString("creation_date"),
                rs.getString("last_viewed")
        );
    }
}
