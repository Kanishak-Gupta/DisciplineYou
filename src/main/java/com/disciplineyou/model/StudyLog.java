package com.disciplineyou.model;

public class StudyLog {
    private int id;
    private int dailyLogId;
    private int subjectId;
    private int durationMins;

    // Transient display field
    private String subjectName;

    public StudyLog() {}

    public StudyLog(int dailyLogId, int subjectId, int durationMins) {
        this.dailyLogId = dailyLogId;
        this.subjectId = subjectId;
        this.durationMins = durationMins;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDailyLogId() { return dailyLogId; }
    public void setDailyLogId(int dailyLogId) { this.dailyLogId = dailyLogId; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public int getDurationMins() { return durationMins; }
    public void setDurationMins(int durationMins) { this.durationMins = durationMins; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }
}
