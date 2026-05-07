package com.disciplineyou.model;

import java.time.LocalDate;

/**
 * Log entry for a missed/absent class.
 */
public class AbsentLog {
    private int id;
    private int subjectId;
    private LocalDate absentDate;
    private String reason;   // SICK, PERSONAL_WORK, EMERGENCY, OTHER
    private String notes;

    // Transient
    private String subjectName;

    public AbsentLog() {}

    public AbsentLog(int subjectId, LocalDate absentDate, String reason, String notes) {
        this.subjectId = subjectId;
        this.absentDate = absentDate;
        this.reason = reason;
        this.notes = notes;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public LocalDate getAbsentDate() { return absentDate; }
    public void setAbsentDate(LocalDate absentDate) { this.absentDate = absentDate; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    /** Get human-readable reason */
    public String getReasonDisplay() {
        return switch (reason) {
            case "SICK" -> "🤒 Sick";
            case "PERSONAL_WORK" -> "📋 Personal Work";
            case "EMERGENCY" -> "🚨 Emergency";
            case "OTHER" -> "📝 Other";
            default -> reason;
        };
    }
}
