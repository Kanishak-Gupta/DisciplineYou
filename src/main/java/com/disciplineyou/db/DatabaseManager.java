package com.disciplineyou.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 * Singleton database manager for SQLite.
 * Creates the database file and all tables on first use.
 */
public class DatabaseManager {

    private static final String DB_URL = "jdbc:sqlite:disciplineyou.db";
    private static DatabaseManager instance;
    private Connection connection;

    private DatabaseManager() {
        try {
            connection = DriverManager.getConnection(DB_URL);
            createTables();
            runMigrations();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize database", e);
        }
    }

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    public Connection getConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(DB_URL);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to get database connection", e);
        }
        return connection;
    }

    private void createTables() {
        try (Statement stmt = connection.createStatement()) {
            // Enable foreign keys
            stmt.execute("PRAGMA foreign_keys = ON");

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS semesters (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL,
                    start_date DATE NOT NULL,
                    end_date DATE NOT NULL,
                    is_active INTEGER DEFAULT 0
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subjects (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    semester_id INTEGER NOT NULL,
                    name TEXT NOT NULL,
                    short_code TEXT,
                    faculty TEXT,
                    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS goals (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    parent_id INTEGER,
                    type TEXT NOT NULL,
                    title TEXT NOT NULL,
                    description TEXT,
                    target_date DATE NOT NULL,
                    original_date DATE,
                    status TEXT DEFAULT 'PENDING',
                    shift_count INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (parent_id) REFERENCES goals(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS daily_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    log_date DATE NOT NULL UNIQUE,
                    social_media_mins INTEGER DEFAULT 0,
                    movie_mins INTEGER DEFAULT 0,
                    college_mins INTEGER DEFAULT 0,
                    notes TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS study_logs (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    daily_log_id INTEGER NOT NULL,
                    subject_id INTEGER NOT NULL,
                    duration_mins INTEGER DEFAULT 0,
                    FOREIGN KEY (daily_log_id) REFERENCES daily_logs(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS timetable (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    semester_id INTEGER NOT NULL,
                    day_of_week INTEGER NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    subject_id INTEGER NOT NULL,
                    room TEXT,
                    faculty TEXT,
                    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS timetable_overrides (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timetable_id INTEGER NOT NULL,
                    override_date DATE NOT NULL,
                    override_type TEXT NOT NULL,
                    new_subject_id INTEGER,
                    new_room TEXT,
                    new_faculty TEXT,
                    note TEXT,
                    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
                    FOREIGN KEY (new_subject_id) REFERENCES subjects(id) ON DELETE SET NULL
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS attendance (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    timetable_id INTEGER NOT NULL,
                    class_date DATE NOT NULL,
                    status TEXT NOT NULL,
                    FOREIGN KEY (timetable_id) REFERENCES timetable(id) ON DELETE CASCADE,
                    UNIQUE(timetable_id, class_date)
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS extra_classes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    semester_id INTEGER NOT NULL,
                    class_date DATE NOT NULL,
                    start_time TEXT NOT NULL,
                    end_time TEXT NOT NULL,
                    subject_id INTEGER NOT NULL,
                    room TEXT,
                    faculty TEXT,
                    status TEXT,
                    FOREIGN KEY (semester_id) REFERENCES semesters(id) ON DELETE CASCADE,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            // ── Existing task system tables ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    week_start_date TEXT NOT NULL,
                    day_specific INTEGER DEFAULT 0,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            stmt.execute("""
                CREATE TABLE IF NOT EXISTS task_completions (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    task_id INTEGER NOT NULL,
                    completion_date TEXT NOT NULL,
                    completed INTEGER DEFAULT 0,
                    FOREIGN KEY (task_id) REFERENCES tasks(id) ON DELETE CASCADE,
                    UNIQUE(task_id, completion_date)
                )
            """);

            // ══════════════════════════════════════════════════
            //  NEW TABLES
            // ══════════════════════════════════════════════════

            // ── Goal Tasks (hierarchical task management) ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS goal_tasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    title TEXT NOT NULL,
                    description TEXT,
                    start_date DATE NOT NULL,
                    target_date DATE NOT NULL,
                    priority TEXT DEFAULT 'MEDIUM',
                    progress_percent INTEGER DEFAULT 0,
                    category TEXT,
                    status TEXT DEFAULT 'ACTIVE',
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                    completed_at DATE
                )
            """);

            // ── Subtasks ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS subtasks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    goal_task_id INTEGER NOT NULL,
                    week_number INTEGER NOT NULL,
                    day_date DATE NOT NULL,
                    title TEXT NOT NULL,
                    completed INTEGER DEFAULT 0,
                    completed_at DATETIME,
                    FOREIGN KEY (goal_task_id) REFERENCES goal_tasks(id) ON DELETE CASCADE
                )
            """);

            // ── Time Entries ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS time_entries (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    activity_name TEXT NOT NULL,
                    duration_mins INTEGER NOT NULL,
                    entry_date DATE NOT NULL,
                    category TEXT,
                    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
                )
            """);

            // ── Absent Class Log ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS absent_log (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    subject_id INTEGER NOT NULL,
                    absent_date DATE NOT NULL,
                    reason TEXT NOT NULL,
                    notes TEXT,
                    FOREIGN KEY (subject_id) REFERENCES subjects(id) ON DELETE CASCADE
                )
            """);

            // ── Achievements ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS achievements (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    name TEXT NOT NULL UNIQUE,
                    description TEXT,
                    icon TEXT,
                    type TEXT NOT NULL,
                    threshold INTEGER DEFAULT 0,
                    unlocked_at DATE
                )
            """);

            // ── Streaks ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS streaks (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    streak_date DATE NOT NULL UNIQUE,
                    all_completed INTEGER DEFAULT 0
                )
            """);

            // ── App Settings (for dark mode, preferences, etc.) ──
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS app_settings (
                    key TEXT PRIMARY KEY,
                    value TEXT
                )
            """);

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
        }
    }

    /**
     * Run schema migrations for existing tables.
     * Each migration is idempotent (safe to run multiple times).
     */
    private void runMigrations() {
        try (Statement stmt = connection.createStatement()) {
            // Add notes column to timetable if it doesn't exist
            try {
                stmt.execute("ALTER TABLE timetable ADD COLUMN notes TEXT");
            } catch (SQLException e) {
                // Column already exists — ignore
            }
        } catch (SQLException e) {
            System.err.println("Migration warning: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            System.err.println("Error closing database: " + e.getMessage());
        }
    }

    // ── Settings helpers ──

    public String getSetting(String key) {
        try (var ps = getConnection().prepareStatement("SELECT value FROM app_settings WHERE key=?")) {
            ps.setString(1, key);
            try (var rs = ps.executeQuery()) {
                if (rs.next()) return rs.getString("value");
            }
        } catch (SQLException e) {
            System.err.println("Error reading setting: " + e.getMessage());
        }
        return null;
    }

    public void setSetting(String key, String value) {
        try (var ps = getConnection().prepareStatement(
                "INSERT INTO app_settings (key, value) VALUES (?, ?) ON CONFLICT(key) DO UPDATE SET value=excluded.value")) {
            ps.setString(1, key);
            ps.setString(2, value);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Error saving setting: " + e.getMessage());
        }
    }
}
