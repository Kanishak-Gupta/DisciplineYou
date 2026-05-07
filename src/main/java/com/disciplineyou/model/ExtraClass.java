package com.disciplineyou.model;

import java.time.LocalDate;

public class ExtraClass {
    private int id;
    private int semesterId;
    private LocalDate classDate;
    private String startTime;
    private String endTime;
    private int subjectId;
    private String room;
    private String faculty;
    private String status; // 'PRESENT', 'ABSENT', or null

    // Transient display fields
    private String subjectName;
    private String subjectShortCode;

    public ExtraClass() {}

    public ExtraClass(int semesterId, LocalDate classDate, String startTime, String endTime,
                      int subjectId, String room, String faculty) {
        this.semesterId = semesterId;
        this.classDate = classDate;
        this.startTime = startTime;
        this.endTime = endTime;
        this.subjectId = subjectId;
        this.room = room;
        this.faculty = faculty;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getSemesterId() { return semesterId; }
    public void setSemesterId(int semesterId) { this.semesterId = semesterId; }

    public LocalDate getClassDate() { return classDate; }
    public void setClassDate(LocalDate classDate) { this.classDate = classDate; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }

    public int getSubjectId() { return subjectId; }
    public void setSubjectId(int subjectId) { this.subjectId = subjectId; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getSubjectName() { return subjectName; }
    public void setSubjectName(String subjectName) { this.subjectName = subjectName; }

    public String getSubjectShortCode() { return subjectShortCode; }
    public void setSubjectShortCode(String subjectShortCode) { this.subjectShortCode = subjectShortCode; }
}
