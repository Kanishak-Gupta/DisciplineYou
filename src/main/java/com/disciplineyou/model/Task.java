package com.disciplineyou.model;

import java.time.LocalDate;

/**
 * A task that can be assigned to a whole week or a specific day.
 * Tasks are organized by week (week_start_date = Monday of the week).
 */
public class Task {
    private int id;
    private String title;
    private LocalDate weekStartDate;  // Monday of the week
    private int daySpecific;          // 0=whole week, 1=Mon..7=Sun for specific day
    private String createdAt;

    public Task() {}

    /** Create a whole-week task */
    public Task(String title, LocalDate weekStartDate) {
        this.title = title;
        this.weekStartDate = weekStartDate;
        this.daySpecific = 0;
    }

    /** Create a day-specific task */
    public Task(String title, LocalDate weekStartDate, int daySpecific) {
        this.title = title;
        this.weekStartDate = weekStartDate;
        this.daySpecific = daySpecific;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getWeekStartDate() { return weekStartDate; }
    public void setWeekStartDate(LocalDate weekStartDate) { this.weekStartDate = weekStartDate; }

    public int getDaySpecific() { return daySpecific; }
    public void setDaySpecific(int daySpecific) { this.daySpecific = daySpecific; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    /** True if this task applies to every day of the week */
    public boolean isWholeWeek() { return daySpecific == 0; }

    /** Check if this task applies to a given day of week (1=Mon..7=Sun) */
    public boolean appliesTo(int dayOfWeek) {
        return daySpecific == 0 || daySpecific == dayOfWeek;
    }

    @Override
    public String toString() { return title; }
}
