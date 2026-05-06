package com.disciplineyou.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class DailyLog {
    private int id;
    private LocalDate logDate;
    private int socialMediaMins;
    private int movieMins;
    private int collegeMins;
    private String notes;
    private LocalDateTime createdAt;

    public DailyLog() {}

    public DailyLog(LocalDate logDate) {
        this.logDate = logDate;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public LocalDate getLogDate() { return logDate; }
    public void setLogDate(LocalDate logDate) { this.logDate = logDate; }

    public int getSocialMediaMins() { return socialMediaMins; }
    public void setSocialMediaMins(int socialMediaMins) { this.socialMediaMins = socialMediaMins; }

    public int getMovieMins() { return movieMins; }
    public void setMovieMins(int movieMins) { this.movieMins = movieMins; }

    public int getCollegeMins() { return collegeMins; }
    public void setCollegeMins(int collegeMins) { this.collegeMins = collegeMins; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    /** Convert minutes to a human-friendly string like "2h 30m" */
    public static String formatMinutes(int mins) {
        if (mins <= 0) return "0m";
        int h = mins / 60;
        int m = mins % 60;
        if (h > 0 && m > 0) return h + "h " + m + "m";
        if (h > 0) return h + "h";
        return m + "m";
    }
}
