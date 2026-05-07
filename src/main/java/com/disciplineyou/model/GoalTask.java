package com.disciplineyou.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * A long-term goal-based task with hierarchical subtask breakdown.
 * Supports week-wise and day-wise subtask division with progress tracking.
 */
public class GoalTask {
    private int id;
    private String title;
    private String description;
    private LocalDate startDate;
    private LocalDate targetDate;
    private String priority;       // HIGH, MEDIUM, LOW
    private int progressPercent;   // 0–100
    private String category;       // tag string
    private String status;         // ACTIVE, COMPLETED, ARCHIVED
    private LocalDateTime createdAt;
    private LocalDate completedAt;

    public GoalTask() {
        this.status = "ACTIVE";
        this.progressPercent = 0;
        this.priority = "MEDIUM";
    }

    public GoalTask(String title, String description, LocalDate startDate, LocalDate targetDate,
                    String priority, String category) {
        this();
        this.title = title;
        this.description = description;
        this.startDate = startDate;
        this.targetDate = targetDate;
        this.priority = priority;
        this.category = category;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getProgressPercent() { return progressPercent; }
    public void setProgressPercent(int progressPercent) { this.progressPercent = progressPercent; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDate getCompletedAt() { return completedAt; }
    public void setCompletedAt(LocalDate completedAt) { this.completedAt = completedAt; }

    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isArchived() { return "ARCHIVED".equals(status); }
    public boolean isActive() { return "ACTIVE".equals(status); }

    /** Check if this task is overdue */
    public boolean isOverdue() {
        return isActive() && targetDate != null && targetDate.isBefore(LocalDate.now());
    }

    /** Get the priority color hex */
    public String getPriorityColor() {
        return switch (priority) {
            case "HIGH" -> "#F44336";
            case "MEDIUM" -> "#FF9800";
            case "LOW" -> "#4CAF50";
            default -> "#9E9E9E";
        };
    }

    @Override
    public String toString() { return title; }
}
