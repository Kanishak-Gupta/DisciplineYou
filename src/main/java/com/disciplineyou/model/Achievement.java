package com.disciplineyou.model;

import java.time.LocalDate;

/**
 * Gamification achievement/badge.
 */
public class Achievement {
    private int id;
    private String name;
    private String description;
    private String icon;
    private String type;          // STREAK, TASK_COUNT, PERFECT_WEEK, CONSISTENCY
    private int threshold;        // e.g., 7 for "7-Day Consistency"
    private LocalDate unlockedAt;

    public Achievement() {}

    public Achievement(String name, String description, String icon, String type, int threshold) {
        this.name = name;
        this.description = description;
        this.icon = icon;
        this.type = type;
        this.threshold = threshold;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public int getThreshold() { return threshold; }
    public void setThreshold(int threshold) { this.threshold = threshold; }

    public LocalDate getUnlockedAt() { return unlockedAt; }
    public void setUnlockedAt(LocalDate unlockedAt) { this.unlockedAt = unlockedAt; }

    public boolean isUnlocked() { return unlockedAt != null; }

    @Override
    public String toString() { return icon + " " + name; }
}
