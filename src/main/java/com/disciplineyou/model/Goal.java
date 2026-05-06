package com.disciplineyou.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class Goal {
    private int id;
    private Integer parentId;  // null for top-level monthly goals
    private String type;       // MONTHLY, WEEKLY, DAILY
    private String title;
    private String description;
    private LocalDate targetDate;
    private LocalDate originalDate;
    private String status;     // PENDING, COMPLETED, SHIFTED
    private int shiftCount;
    private LocalDateTime createdAt;

    public Goal() {
        this.status = "PENDING";
        this.shiftCount = 0;
    }

    public Goal(String type, String title, LocalDate targetDate) {
        this();
        this.type = type;
        this.title = title;
        this.targetDate = targetDate;
        this.originalDate = targetDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public Integer getParentId() { return parentId; }
    public void setParentId(Integer parentId) { this.parentId = parentId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public LocalDate getTargetDate() { return targetDate; }
    public void setTargetDate(LocalDate targetDate) { this.targetDate = targetDate; }

    public LocalDate getOriginalDate() { return originalDate; }
    public void setOriginalDate(LocalDate originalDate) { this.originalDate = originalDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public int getShiftCount() { return shiftCount; }
    public void setShiftCount(int shiftCount) { this.shiftCount = shiftCount; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public boolean isCompleted() { return "COMPLETED".equals(status); }
    public boolean isShifted() { return shiftCount > 0; }

    @Override
    public String toString() {
        return title;
    }
}
