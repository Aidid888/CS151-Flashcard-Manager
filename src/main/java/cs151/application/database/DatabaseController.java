package cs151.application.database;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseController {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseController.class);
    private static DatabaseController instance;

    private final String dbPath;
    private Connection connection;

    private static final String CREATE_DECK_TABLE = """
            CREATE TABLE IF NOT EXISTS Deck_table (
                id          INTEGER PRIMARY KEY AUTOINCREMENT,
                deck_name   TEXT    NOT NULL UNIQUE,
                description TEXT
            );
            """;

    private static final String CREATE_FLASHCARD_TABLE = """
            CREATE TABLE IF NOT EXISTS Flashcard_table (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                deck_id       INTEGER NOT NULL,
                deck_name     TEXT    NOT NULL,
                front_text    TEXT    NOT NULL,
                back_text     TEXT    NOT NULL,
                status        TEXT    NOT NULL DEFAULT 'new',
                creation_date TEXT    NOT NULL DEFAULT (datetime('now')),
                last_viewed   TEXT,
                FOREIGN KEY (deck_name) REFERENCES Deck_table(deck_name)
                    ON UPDATE CASCADE ON DELETE CASCADE
            );
            """;

    /**
     * Creates the singleton instance of DatabaseController.
     * Must be called once at app startup (in Main.java) before any DAO is used.
     * Calling this again after initialization has no effect.
     */
    public static void initialize(String dbPath) {
        if (instance == null) {
            instance = new DatabaseController(dbPath);
        }
    }

    /**
     * Returns the shared singleton instance.
     * Throws IllegalStateException if initialize() was never called.
     */
    public static DatabaseController getInstance() {
        if (instance == null) {
            throw new IllegalStateException("DatabaseController not initialized. Call initialize() first.");
        }
        return instance;
    }

    /**
     * Private constructor — opens the connection and creates tables.
     * Cannot be called directly; use initialize() instead.
     */
    private DatabaseController(String dbPath) {
        this.dbPath = dbPath;
        connect();
        createTables();
    }

    /**
     * Opens a JDBC connection to the SQLite file at dbPath.
     * Also enables foreign key enforcement, which SQLite has OFF by default.
     */
    private void connect() {
        try {
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("PRAGMA foreign_keys = ON;");
            }
            logger.info("Connected to SQLite database at: {}", dbPath);
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection failed.", e);
        }
    }

    /**
     * Creates Deck_table and Flashcard_table if they don't already exist.
     * Safe to call on every startup — IF NOT EXISTS prevents duplicates.
     */
    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_DECK_TABLE);
            logger.info("Deck_table ready.");
            stmt.execute(CREATE_FLASHCARD_TABLE);
            logger.info("Flashcard_table ready.");
        } catch (SQLException e) {
            logger.error("Failed to create tables: {}", e.getMessage(), e);
            throw new RuntimeException("Table creation failed.", e);
        }
    }

    /**
     * Returns the active Connection object.
     * Used by DeckDao and FlashcardDao to run their SQL queries.
     */
    public Connection getConnection() {
        return connection;
    }

    /**
     * Closes the database connection gracefully.
     * Should be called when the app shuts down via Main.stop().
     */
    public void close() {
        if (connection != null) {
            try {
                connection.close();
                logger.info("Database connection closed.");
            } catch (SQLException e) {
                logger.error("Failed to close connection: {}", e.getMessage(), e);
            }
        }
    }
}
