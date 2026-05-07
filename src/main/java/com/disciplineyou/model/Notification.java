package com.disciplineyou.model;

import java.time.LocalDateTime;

/**
 * In-memory notification for centralized alerts.
 */
public class Notification {

    public enum Type { MISSED_TASK, DEADLINE, TIMETABLE_CHANGE, ATTENDANCE_WARNING }

    private Type type;
    private String title;
    private String message;
    private LocalDateTime timestamp;
    private boolean read;

    public Notification() {
        this.timestamp = LocalDateTime.now();
        this.read = false;
    }

    public Notification(Type type, String title, String message) {
        this();
        this.type = type;
        this.title = title;
        this.message = message;
    }

    // Getters and Setters
    public Type getType() { return type; }
    public void setType(Type type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }

    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }

    /** Get icon for notification type */
    public String getIcon() {
        return switch (type) {
            case MISSED_TASK -> "⚠";
            case DEADLINE -> "⏰";
            case TIMETABLE_CHANGE -> "🔄";
            case ATTENDANCE_WARNING -> "📉";
        };
    }
}
