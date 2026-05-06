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

            // ── New task system tables ──
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

        } catch (SQLException e) {
            throw new RuntimeException("Failed to create tables", e);
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
}
