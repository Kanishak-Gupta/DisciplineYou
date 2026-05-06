package com.disciplineyou.model;

import java.time.LocalDate;

/**
 * Tracks whether a task is completed on a specific date.
 * One record per (task, date) pair.
 */
public class TaskCompletion {
    private int id;
    private int taskId;
    private LocalDate completionDate;
    private boolean completed;

    public TaskCompletion() {}

    public TaskCompletion(int taskId, LocalDate completionDate, boolean completed) {
        this.taskId = taskId;
        this.completionDate = completionDate;
        this.completed = completed;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTaskId() { return taskId; }
    public void setTaskId(int taskId) { this.taskId = taskId; }

    public LocalDate getCompletionDate() { return completionDate; }
    public void setCompletionDate(LocalDate completionDate) { this.completionDate = completionDate; }

    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
