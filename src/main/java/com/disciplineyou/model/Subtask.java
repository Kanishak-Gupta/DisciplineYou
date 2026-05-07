package com.disciplineyou.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A subtask belonging to a GoalTask.
 * Can be week-scoped or day-specific with checkbox completion tracking.
 */
public class Subtask {
    private int id;
    private int goalTaskId;
    private int weekNumber;        // which dynamic week segment (1-based)
    private LocalDate dayDate;     // specific day this subtask is for
    private String title;
    private boolean completed;
    private LocalDateTime completedAt;

    public Subtask() {}

    public Subtask(int goalTaskId, int weekNumber, LocalDate dayDate, String title) {
        this.goalTaskId = goalTaskId;
        this.weekNumber = weekNumber;
        this.dayDate = dayDate;
        this.title = title;
        this.completed = false;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getGoalTaskId() { return goalTaskId; }
    public void setGoalTaskId(int goalTaskId) { this.goalTaskId = goalTaskId; }

    public int getWeekNumber() { return weekNumber; }
    public void setWeekNumber(int weekNumber) { this.weekNumber = weekNumber; }

    public LocalDate getDayDate() { return dayDate; }
    public void setDayDate(LocalDate dayDate) { this.dayDate = dayDate; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }

    public LocalDateTime getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDateTime completedAt) { this.completedAt = completedAt; }

    /** Check if this subtask is overdue (past day, not completed) */
    public boolean isOverdue() {
        return !completed && dayDate != null && dayDate.isBefore(LocalDate.now());
    }

    @Override
    public String toString() { return title; }
}
