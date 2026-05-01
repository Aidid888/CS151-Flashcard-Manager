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
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                deck_name     TEXT    NOT NULL UNIQUE,
                description   TEXT,
                creation_date TEXT    NOT NULL DEFAULT (datetime('now')),
                last_visited  TEXT
            );
            """;

    private static final String CREATE_FLASHCARD_TABLE = """
            CREATE TABLE IF NOT EXISTS Flashcard_table (
                id            INTEGER PRIMARY KEY AUTOINCREMENT,
                deck_id       INTEGER NOT NULL,
                deck_name     TEXT    NOT NULL,
                front_text    TEXT    NOT NULL,
                back_text     TEXT    NOT NULL,
                status        TEXT    NOT NULL DEFAULT 'New'
                                    CHECK (status IN ('New', 'Learning', 'Mastered')),
                creation_date TEXT    NOT NULL DEFAULT (datetime('now')),
                last_viewed   TEXT,
                FOREIGN KEY (deck_id) REFERENCES Deck_table(id)
                    ON UPDATE CASCADE ON DELETE CASCADE
            );
            """;

    //Index on deck_id for faster flashcard lookups by deck
    private static final String CREATE_FLASHCARD_DECK_INDEX = """
            CREATE INDEX IF NOT EXISTS idx_flashcard_deck_id
                ON Flashcard_table(deck_id);
            """;

    /**
     * Creates the singleton instance of DatabaseController.
     * Must be called once at app startup (in Main.java) before any DAO is used.
     * Calling this again after initialization has no effect.
     */
    public static synchronized void initialize(String dbPath) {
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
     * Returns true if the shared connection is open and responsive.
     * Uses a 2-second timeout for the isValid() check.
     */
    private boolean isConnectionValid() {
        try {
            return connection != null && !connection.isClosed() && connection.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Attempts to reconnect the shared connection if it has gone stale.
     * Closes the old connection first if it's still technically open.
     */
    private synchronized void reconnect() {
        logger.warn("Shared connection is invalid. Attempting to reconnect...");
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException ignored) {}
        connection = null;
        connect();
        logger.info("Reconnection successful.");
    }

    /**
     * Opens a JDBC connection to the SQLite file at dbPath.
     * Also enables WAL journal mode for better read/write concurrency,
     * and enforces foreign key constraints (SQLite has both OFF by default).
     */
    private void connect() {
        try {
            String url = "jdbc:sqlite:" + dbPath;
            connection = DriverManager.getConnection(url);
            try (Statement stmt = connection.createStatement()) {
                // FIX #7: Enable WAL mode for better concurrent read/write performance
                stmt.execute("PRAGMA journal_mode=WAL;");

                stmt.execute("PRAGMA foreign_keys = ON;");
                try (var rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                    if (rs.next() && rs.getInt(1) != 1) {
                        throw new RuntimeException("Failed to enable foreign key enforcement.");
                    }
                }
            }
            logger.info("Connected to SQLite database at: {}", dbPath);
        } catch (SQLException e) {
            logger.error("Failed to connect to database: {}", e.getMessage(), e);
            throw new RuntimeException("Database connection failed.", e);
        }
    }

    /**
     * Creates Deck_table and Flashcard_table if they don't already exist.
     * Also migrates existing databases to add new columns if missing.
     * Safe to call on every startup — IF NOT EXISTS prevents duplicates.
     */
    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute(CREATE_DECK_TABLE);
            logger.info("Deck_table ready.");
            stmt.execute(CREATE_FLASHCARD_TABLE);
            logger.info("Flashcard_table ready.");
            stmt.execute(CREATE_FLASHCARD_DECK_INDEX);
            logger.info("Flashcard deck_id index ready.");

            // Migrate existing databases — safe to run every startup
            try { stmt.execute("ALTER TABLE Deck_table ADD COLUMN creation_date TEXT NOT NULL DEFAULT (datetime('now'))"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE Deck_table ADD COLUMN last_visited TEXT"); } catch (SQLException ignored) {}

            // Migrate Flashcard_table
            try { stmt.execute("ALTER TABLE Flashcard_table ADD COLUMN creation_date TEXT DEFAULT (datetime('now'))"); } catch (SQLException ignored) {}
            try { stmt.execute("ALTER TABLE Flashcard_table ADD COLUMN last_viewed TEXT"); } catch (SQLException ignored) {}
        } catch (SQLException e) {
            logger.error("Failed to create tables: {}", e.getMessage(), e);
            throw new RuntimeException("Table creation failed.", e);
        }
    }

    /**
     * Returns the active shared Connection object.
     * Automatically reconnects if the connection has gone stale.
     */
    public synchronized Connection getConnection() {
        if (!isConnectionValid()) {
            reconnect();
        }
        return connection;
    }

    /**
     * Opens and returns a new short-lived connection to the SQLite database.
     * The caller is responsible for closing it — always use in a try-with-resources.
     * Used by DAOs for per-operation connections instead of the shared connection.
     */
    public Connection openConnection() throws SQLException {
        String url = "jdbc:sqlite:" + dbPath;
        Connection conn = DriverManager.getConnection(url);
        try (Statement stmt = conn.createStatement()) {
            stmt.execute("PRAGMA journal_mode=WAL;");
            stmt.execute("PRAGMA foreign_keys = ON;");
            try (var rs = stmt.executeQuery("PRAGMA foreign_keys;")) {
                if (rs.next() && rs.getInt(1) != 1) {
                    conn.close();
                    throw new SQLException("Failed to enable foreign key enforcement.");
                }
            }
        }
        logger.debug("Opened per-operation connection to: {}", dbPath);
        return conn;
    }

    /**
     * Closes the database connection gracefully.
     * Should be called when the app shuts down via Main.stop().
     */
    public synchronized void close() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                logger.info("Database connection closed.");
            } catch (SQLException e) {
                logger.error("Failed to close connection: {}", e.getMessage(), e);
            }
        }
    }
}