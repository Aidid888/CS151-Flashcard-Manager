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
     * Record fields accessed via id(), deckId(), deckName(), frontText(),
     * backText(), status(), creationDate(), lastViewed().
     * lastViewed() may be null if the card has never been reviewed.
     */
    public record Flashcard(
            int id,
            int deckId,
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
     * Inserts a new flashcard into Flashcard_table linked to the given deck.
     * creation_date is set automatically by the database via DEFAULT (datetime('now')).
     * last_viewed is left null — it is only set when the card is actually reviewed.
     * front_text, back_text, and status are all required (NOT NULL in schema).
     */
    public void insertFlashcard(int deckId, String deckName, String frontText,
                                String backText, String status) throws SQLException {
        String sql = """
                INSERT INTO Flashcard_table (deck_id, deck_name, front_text, back_text, status)
                VALUES (?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            stmt.setString(2, deckName);
            stmt.setString(3, frontText);
            stmt.setString(4, backText);
            stmt.setString(5, status);
            stmt.executeUpdate();
            logger.info("Inserted flashcard into deck_id: {}", deckId);
        }
    }

    // ---------------------------------------------------------
    // READ
    // ---------------------------------------------------------

    /**
     * Returns all flashcards belonging to a deck, ordered by creation_date ascending
     * (oldest first). Returns an empty list if the deck has no flashcards.
     */
    public List<Flashcard> getFlashcardsByDeckId(int deckId) throws SQLException {
        String sql = """
                SELECT id, deck_id, deck_name, front_text, back_text, status, creation_date, last_viewed
                FROM Flashcard_table
                WHERE deck_id = ?
                ORDER BY creation_date ASC
                """;
        List<Flashcard> flashcards = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    flashcards.add(mapRow(rs));
                }
            }
        }
        return flashcards;
    }

    /**
     * Finds and returns a single flashcard by its primary key.
     * Returns null if no flashcard with that id exists.
     */
    public Flashcard getFlashcardById(int flashcardId) throws SQLException {
        String sql = """
                SELECT id, deck_id, deck_name, front_text, back_text, status, creation_date, last_viewed
                FROM Flashcard_table
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flashcardId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRow(rs);
                }
            }
        }
        return null;
    }

    /**
     * Returns all flashcards in a deck that match a given status (e.g. "new",
     * "learning", "mastered"). Useful for filtering cards during a study session.
     * Returns an empty list if no cards match.
     */
    public List<Flashcard> getFlashcardsByStatus(int deckId, String status) throws SQLException {
        String sql = """
                SELECT id, deck_id, deck_name, front_text, back_text, status, creation_date, last_viewed
                FROM Flashcard_table
                WHERE deck_id = ? AND status = ?
                ORDER BY creation_date ASC
                """;
        List<Flashcard> flashcards = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            stmt.setString(2, status);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    flashcards.add(mapRow(rs));
                }
            }
        }
        return flashcards;
    }

    /**
     * Returns the total number of flashcards in a deck.
     * Useful for displaying a card count in the deck list UI
     * without fetching full card data.
     */
    public int getFlashcardCountByDeckId(int deckId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM Flashcard_table WHERE deck_id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, deckId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        return 0;
    }

    // ---------------------------------------------------------
    // UPDATE
    // ---------------------------------------------------------

    /**
     * Updates the front text, back text, and status of an existing flashcard.
     * Use this when the user edits the full card content.
     * Does NOT touch creation_date or last_viewed.
     */
    public void updateFlashcard(int flashcardId, String frontText,
                                String backText, String status) throws SQLException {
        String sql = """
                UPDATE Flashcard_table
                SET front_text = ?, back_text = ?, status = ?
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, frontText);
            stmt.setString(2, backText);
            stmt.setString(3, status);
            stmt.setInt(4, flashcardId);
            stmt.executeUpdate();
            logger.info("Updated flashcard id: {}", flashcardId);
        }
    }

    /**
     * Stamps last_viewed with the current database time.
     * Call this every time the user flips or reviews a card during a study session.
     */
    public void updateLastViewed(int flashcardId) throws SQLException {
        String sql = "UPDATE Flashcard_table SET last_viewed = datetime('now') WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flashcardId);
            stmt.executeUpdate();
            logger.info("Updated last_viewed for flashcard id: {}", flashcardId);
        }
    }

    /**
     * Updates only the status field of a flashcard (e.g. "new" → "learning" → "mastered").
     * Preferred over calling updateFlashcard() for status-only changes, as it avoids
     * overwriting front_text and back_text unnecessarily.
     */
    public void updateStatus(int flashcardId, String status) throws SQLException {
        String sql = "UPDATE Flashcard_table SET status = ? WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, status);
            stmt.setInt(2, flashcardId);
            stmt.executeUpdate();
            logger.info("Updated status for flashcard id: {} → {}", flashcardId, status);
        }
    }

    // ---------------------------------------------------------
    // DELETE
    // ---------------------------------------------------------

    /**
     * Deletes a single flashcard by its primary key.
     * Does not affect the parent deck or any other flashcards.
     */
    public void deleteFlashcard(int flashcardId) throws SQLException {
        String sql = "DELETE FROM Flashcard_table WHERE id = ?";
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, flashcardId);
            stmt.executeUpdate();
            logger.info("Deleted flashcard id: {}", flashcardId);
        }
    }

    // ---------------------------------------------------------
    // PRIVATE HELPERS
    // ---------------------------------------------------------

    /**
     * Maps the current ResultSet row to a Flashcard record.
     * Centralizes column name mapping so all read methods stay DRY.
     */
    private Flashcard mapRow(ResultSet rs) throws SQLException {
        return new Flashcard(
                rs.getInt("id"),
                rs.getInt("deck_id"),
                rs.getString("deck_name"),
                rs.getString("front_text"),
                rs.getString("back_text"),
                rs.getString("status"),
                rs.getString("creation_date"),
                rs.getString("last_viewed")
        );
    }
}
