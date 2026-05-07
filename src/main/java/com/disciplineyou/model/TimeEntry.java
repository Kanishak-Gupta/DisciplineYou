package com.disciplineyou.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A time entry for tracking how much time was spent on an activity.
 */
public class TimeEntry {
    private int id;
    private String activityName;
    private int durationMins;
    private LocalDate entryDate;
    private String category;    // e.g., Study, Leisure, Exercise, Work
    private LocalDateTime createdAt;

    public TimeEntry() {}

    public TimeEntry(String activityName, int durationMins, LocalDate entryDate, String category) {
        this.activityName = activityName;
        this.durationMins = durationMins;
        this.entryDate = entryDate;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getActivityName() { return activityName; }
    public void setActivityName(String activityName) { this.activityName = activityName; }

    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }

    public LocalDate getEntryDate() { return entryDate; }
    public void setEntryDate(LocalDate entryDate) { this.entryDate = entryDate; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Format duration as "Xh Ym" */
    public String getFormattedDuration() {
        int h = durationMins / 60;
        int m = durationMins % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        return m + "m";
    }

    @Override
    public String toString() { return activityName + " (" + getFormattedDuration() + ")"; }
}
