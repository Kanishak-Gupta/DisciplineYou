package com.disciplineyou.model;

import java.time.LocalDate;

public class Attendance {
    private int id;
    private int timetableId;
    private LocalDate classDate;
    private String status;  // PRESENT, ABSENT, CANCELLED

    public Attendance() {}

    public Attendance(int timetableId, LocalDate classDate, String status) {
        this.timetableId = timetableId;
        this.classDate = classDate;
        this.status = status;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getTimetableId() { return timetableId; }
    public void setTimetableId(int timetableId) { this.timetableId = timetableId; }

    public LocalDate getClassDate() { return classDate; }
    public void setClassDate(LocalDate classDate) { this.classDate = classDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isPresent() { return "PRESENT".equals(status); }
    public boolean isAbsent() { return "ABSENT".equals(status); }
    public boolean isCancelled() { return "CANCELLED".equals(status); }
}
