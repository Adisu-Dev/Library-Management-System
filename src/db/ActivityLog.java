package db;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Feature 3 — Real-Time Activity Log with Timestamps
 *
 * Design:
 *   • A single static ObservableList<String> holds all log entries.
 *   • Any UI component (ListView, etc.) can bind to getEntries() and will
 *     update automatically whenever a new entry is added.
 *   • log() captures the exact LocalDateTime, formats the entry, then:
 *       1. Calls Platform.runLater() to prepend the entry to the list
 *          (safe from any thread — background tasks, FX thread, etc.).
 *       2. Submits a background task to persist the entry to the DB.
 *   • loadRecent() fetches the last 100 entries from the DB on startup
 *     and populates the list via Platform.runLater().
 *
 * Threading contract:
 *   - All ObservableList mutations happen on the FX thread via Platform.runLater().
 *   - All DB I/O happens on the single-thread background executor.
 *   - Callers may invoke log() from any thread without synchronization.
 */
public class ActivityLog {

    // ── Timestamp format ──────────────────────────────────────────────
    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    // ── Observable list — bind a ListView to this for live updates ────
    private static final ObservableList<String> entries =
        FXCollections.observableArrayList();

    // ── Single-thread executor for all DB writes ──────────────────────
    private static final ExecutorService BG =
        Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "ActivityLog-Writer");
            t.setDaemon(true);   // won't block JVM shutdown
            return t;
        });

    private ActivityLog() {}   // utility class — no instances

    // ─────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────

    /**
     * Log an action performed by a specific user.
     * Safe to call from any thread (FX thread or background thread).
     *
     * @param userId  the ID of the user who performed the action
     * @param message human-readable description of the action
     */
    public static void log(int userId, String message) {
        // Capture timestamp on the calling thread for accuracy
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = "[" + timestamp + "] UserID=" + userId + " — " + message;

        // 1. Update the UI list on the FX thread
        Platform.runLater(() -> {
            entries.add(0, entry);                          // newest first
            if (entries.size() > 200) entries.remove(entries.size() - 1);  // cap at 200
        });

        // 2. Persist to DB on the background thread
        BG.submit(() -> persistToDb(userId, message));
    }

    /**
     * Log a system-level event (no specific user).
     * Convenience overload — uses userId = 0.
     */
    public static void log(String message) {
        log(0, message);
    }

    /**
     * Log a system-level event with an explicit label (e.g. "SYSTEM").
     * Useful for startup/shutdown events.
     */
    public static void logSystem(String message) {
        String timestamp = LocalDateTime.now().format(FMT);
        String entry = "[" + timestamp + "] SYSTEM — " + message;

        Platform.runLater(() -> {
            entries.add(0, entry);
            if (entries.size() > 200) entries.remove(entries.size() - 1);
        });

        BG.submit(() -> persistToDb(0, "[SYSTEM] " + message));
    }

    /**
     * Returns the live ObservableList.
     * Bind a ListView directly to this:
     *   listView.setItems(ActivityLog.getEntries());
     */
    public static ObservableList<String> getEntries() {
        return entries;
    }

    /**
     * Loads the most recent 100 entries from the DB and populates the list.
     * Call once when the dashboard opens.
     * Safe to call from the FX thread — the actual DB work runs in the background.
     */
    public static void loadRecent() {
        BG.submit(() -> {
            try (java.sql.Connection conn = DatabaseConnection.getConnection()) {
                if (conn == null) return;
                ensureTableExists(conn);

                String q = "SELECT TOP 100 LogTime, UserID, Message " +
                           "FROM ActivityLogs ORDER BY LogTime DESC";
                java.sql.ResultSet rs = conn.createStatement().executeQuery(q);

                List<String> loaded = new ArrayList<>();
                while (rs.next()) {
                    String ts  = rs.getTimestamp("LogTime").toString().substring(0, 19);
                    int    uid = rs.getInt("UserID");
                    String msg = rs.getString("Message");
                    loaded.add("[" + ts + "] " +
                        (uid > 0 ? "UserID=" + uid : "SYSTEM") + " — " + msg);
                }

                // Replace the list contents on the FX thread
                Platform.runLater(() -> entries.setAll(loaded));

            } catch (java.sql.SQLException e) {
                System.out.println("ActivityLog.loadRecent error: " + e.getMessage());
            }
        });
    }

    // ─────────────────────────────────────────────────────────────────
    // Internal helpers
    // ─────────────────────────────────────────────────────────────────

    /** Persists a single log entry to the ActivityLogs table. */
    private static void persistToDb(int userId, String message) {
        try (java.sql.Connection conn = DatabaseConnection.getConnection()) {
            if (conn == null) return;
            ensureTableExists(conn);

            try (java.sql.PreparedStatement pst = conn.prepareStatement(
                    "INSERT INTO ActivityLogs (UserID, Message) VALUES (?, ?)")) {
                pst.setInt(1, userId);
                pst.setString(2, message);
                pst.executeUpdate();
            }
        } catch (java.sql.SQLException e) {
            System.out.println("ActivityLog.persistToDb error: " + e.getMessage());
        }
    }

    /**
     * Creates the ActivityLogs table if it doesn't exist yet.
     * Idempotent — safe to call on every connection.
     *
     * SQL Server DDL (run once manually if preferred):
     *   CREATE TABLE ActivityLogs (
     *     LogID   INT PRIMARY KEY IDENTITY(1,1),
     *     LogTime DATETIME DEFAULT GETDATE(),
     *     UserID  INT DEFAULT 0,
     *     Message NVARCHAR(500) NOT NULL
     *   );
     */
    private static void ensureTableExists(java.sql.Connection conn)
            throws java.sql.SQLException {
        conn.createStatement().execute(
            "IF OBJECT_ID('ActivityLogs','U') IS NULL " +
            "CREATE TABLE ActivityLogs (" +
            "  LogID   INT PRIMARY KEY IDENTITY(1,1)," +
            "  LogTime DATETIME DEFAULT GETDATE()," +
            "  UserID  INT DEFAULT 0," +
            "  Message NVARCHAR(500) NOT NULL" +
            ")");
    }
}
